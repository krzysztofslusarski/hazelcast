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

package com.hazelcast.sql.impl.sort;

import java.util.Comparator;
import java.util.List;

/**
 * Comparator fot the sort key.
 */
public class SortKeyComparator implements Comparator<SortKey> {
    /** List of ascending collations. */
    private final List<Boolean> ascs;

    public SortKeyComparator(List<Boolean> ascs) {
        this.ascs = ascs;
    }

    @Override
    public int compare(SortKey o1, SortKey o2) {
        int res;

        for (int i = 0; i < ascs.size(); i++) {
            boolean asc = ascs.get(i);

            Object item1 = o1.getKey().get(i);
            Object item2 = o2.getKey().get(i);

            Comparable item1Comp = (Comparable) item1;
            Comparable item2Comp = (Comparable) item2;

            //noinspection unchecked
            res = item1Comp.compareTo(item2Comp);

            if (!asc) {
                res = -res;
            }

            if (res != 0) {
                if (!asc) {
                    res = -res;
                }

                return res;
            }
        }

        return Long.compare(o1.getIdx(), o2.getIdx());
    }
}
