/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.sql.impl.calcite.schema;

import com.hazelcast.config.IndexConfig;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.map.impl.MapContainer;
import com.hazelcast.map.impl.MapService;
import com.hazelcast.map.impl.proxy.MapProxyImpl;
import com.hazelcast.partition.PartitioningStrategy;
import com.hazelcast.partition.strategy.DeclarativePartitioningStrategy;
import com.hazelcast.replicatedmap.impl.ReplicatedMapService;
import com.hazelcast.spi.impl.NodeEngine;
import com.hazelcast.sql.impl.calcite.statistics.TableStatistics;
import com.hazelcast.sql.impl.calcite.statistics.StatisticProvider;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Utility classes for schema creation.
 */
public final class SchemaUtils {
    /** Name of partitioned schema. */
    public static final String SCHEMA_NAME_PARTITIONED = "partitioned";

    /** Name of replicated schema. */
    public static final String SCHEMA_NAME_REPLICATED = "replicated";

    private SchemaUtils() {
        // No-op.
    }

    /**
     * Creates root schema for the given node engine.
     *
     * @param nodeEngine Node engine.
     * @return Root schema.
     */
    public static HazelcastSchema createRootSchema(NodeEngine nodeEngine, StatisticProvider statisticProvider) {
        // Create partitioned and replicated schemas.
        Map<String, Table> partitionedTables = prepareSchemaTables(nodeEngine, statisticProvider, true);
        Map<String, Table> replicatedTables = prepareSchemaTables(nodeEngine, statisticProvider, false);

        HazelcastSchema partitionedSchema = new HazelcastSchema(partitionedTables);
        HazelcastSchema replicatedSchema = new HazelcastSchema(replicatedTables);

        // Create root schema.
        Map<String, Schema> subSchemaMap = new HashMap<>();

        subSchemaMap.put(SCHEMA_NAME_PARTITIONED, partitionedSchema);
        subSchemaMap.put(SCHEMA_NAME_REPLICATED, replicatedSchema);

        Map<String, Table> tableMap = new HashMap<>();

        for (Map.Entry<String, Table> table : replicatedSchema.getTableMap().entrySet()) {
            tableMap.put(table.getKey(), table.getValue());
        }

        for (Map.Entry<String, Table> table : partitionedSchema.getTableMap().entrySet()) {
            tableMap.put(table.getKey(), table.getValue());
        }

        return new HazelcastSchema(subSchemaMap, tableMap);
    }

    /**
     * Prepare the list of available tables.
     *
     * @param nodeEngine Node engine.
     * @param partitioned {@code True} to prepare the list of partitioned tables, {@code false} to prepare the list
     *     if replicated tables.
     * @return List of tables.
     */
    private static Map<String, Table> prepareSchemaTables(
        NodeEngine nodeEngine,
        StatisticProvider statisticProvider,
        boolean partitioned
    ) {
        String serviceName = partitioned ? MapService.SERVICE_NAME : ReplicatedMapService.SERVICE_NAME;

        Collection<String> mapNames = nodeEngine.getProxyService().getDistributedObjectNames(serviceName);

        HashMap<String, Table> res = new HashMap<>();

        for (String mapName : mapNames) {
            DistributedObject map = nodeEngine.getProxyService().getDistributedObject(
                serviceName,
                mapName,
                nodeEngine.getLocalMember().getUuid()
            );

            long rowCount = statisticProvider.getRowCount(map);

            String distributionField;
            Map<String, String> aliases;
            List<HazelcastTableIndex> indexes;

            if (partitioned) {
                MapProxyImpl map0 = (MapProxyImpl) map;

                PartitioningStrategy strategy = map0.getPartitionStrategy();

                if (strategy instanceof DeclarativePartitioningStrategy) {
                    distributionField = ((DeclarativePartitioningStrategy) strategy).getField();
                } else {
                    distributionField = null;
                }

                aliases = map0.getAttributeAliases();

                indexes = getIndexes(map0);
            } else {
                distributionField = null;
                aliases = null;
                indexes = null;
            }

            HazelcastTable table = new HazelcastTable(
                mapName,
                partitioned,
                distributionField,
                indexes, aliases,
                new TableStatistics(rowCount)
            );

            res.put(mapName, table);
        }

        return res;
    }

    /**
     * Get indexes from the map config.
     *
     * @param map Map.
     * @return Indexes.
     */
    private static List<HazelcastTableIndex> getIndexes(MapProxyImpl map) {
        MapContainer mapContainer = map.getMapServiceContext().getMapContainer(map.getName());

        Collection<IndexConfig> indexConfigs = mapContainer.getIndexDefinitions().values();

        List<HazelcastTableIndex> res = new ArrayList<>(indexConfigs.size());

        for (IndexConfig indexConfig : indexConfigs) {
            boolean duplicate = false;

            for (HazelcastTableIndex index : res) {
                //
                if (Objects.equals(indexConfig.getType(), index.getType())
                    && Objects.equals(indexConfig.getAttributes(), index.getAttributes())) {
                    duplicate = true;

                    break;
                }
            }

            if (duplicate) {
                continue;
            }

            res.add(new HazelcastTableIndex(indexConfig.getName(), indexConfig.getType(), indexConfig.getAttributes()));
        }

        return res;
    }
}
