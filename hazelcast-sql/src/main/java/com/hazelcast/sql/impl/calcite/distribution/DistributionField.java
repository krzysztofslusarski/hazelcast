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

package com.hazelcast.sql.impl.calcite.distribution;

import java.util.Objects;

public class DistributionField {

    private final int index;
    private final String nestedField;

    public DistributionField(int index) {
        this(index, null);
    }

    public DistributionField(int index, String nestedField) {
        this.index = index;
        this.nestedField = nestedField;
    }

    public int getIndex() {
        return index;
    }

    public String getNestedField() {
        return nestedField;
    }

    @Override
    public int hashCode() {
        return 31 * index + (nestedField != null ? nestedField.hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DistributionField) {
            DistributionField other = (DistributionField) obj;

            return index == other.index && Objects.equals(nestedField, other.nestedField);
        }

        return false;
    }

    @Override
    public String toString() {
        return "PhysicalDistributionField{index=" + index + ", nestedField=" + nestedField + '}';
    }
}
