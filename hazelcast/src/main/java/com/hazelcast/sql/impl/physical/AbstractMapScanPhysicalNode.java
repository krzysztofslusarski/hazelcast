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

package com.hazelcast.sql.impl.physical;

import com.hazelcast.internal.serialization.impl.SerializationUtil;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.sql.impl.expression.Expression;

import java.io.IOException;
import java.util.List;

/**
 * Base class to scan a map.
 */
public abstract class AbstractMapScanPhysicalNode extends ZeroInputPhysicalNode implements PhysicalNode {
    /** Map name. */
    protected String mapName;

    /** Field names. */
    protected List<String> fieldNames;

    /** Projects. */
    protected List<Integer> projects;

    /** Filter. */
    protected Expression<Boolean> filter;

    protected AbstractMapScanPhysicalNode() {
        // No-op.
    }

    protected AbstractMapScanPhysicalNode(
        int id,
        String mapName,
        List<String> fieldNames,
        List<Integer> projects,
        Expression<Boolean> filter
    ) {
        super(id);

        this.mapName = mapName;
        this.fieldNames = fieldNames;
        this.projects = projects;
        this.filter = filter;
    }

    public String getMapName() {
        return mapName;
    }

    public List<String> getFieldNames() {
        return fieldNames;
    }

    public List<Integer> getProjects() {
        return projects;
    }

    public Expression<Boolean> getFilter() {
        return filter;
    }

    @Override
    protected void writeData0(ObjectDataOutput out) throws IOException {
        out.writeUTF(mapName);
        SerializationUtil.writeList(fieldNames, out);
        SerializationUtil.writeList(projects, out);
        out.writeObject(filter);
    }

    @Override
    protected void readData0(ObjectDataInput in) throws IOException {
        mapName = in.readUTF();
        fieldNames = SerializationUtil.readList(in);
        projects = SerializationUtil.readList(in);
        filter = in.readObject();
    }
}
