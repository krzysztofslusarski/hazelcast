/*
 * Copyright (c) 2008-2022, Hazelcast, Inc. All Rights Reserved.
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


package com.hazelcast.internal.util.collection;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

public class FixedCapacityArrayList<E> extends AbstractList<E> implements List<E> {
    private static final int DEFAULT_CAPACITY = 10;

    private E[] elementData;
    private int size;

    public FixedCapacityArrayList() {
        this(DEFAULT_CAPACITY);
    }

    public FixedCapacityArrayList(int initialCapacity) {
        this.elementData = (E[]) new Object[initialCapacity];
    }

    @Override
    public boolean add(E e) {
        modCount++;
        if (size == elementData.length) {
            grow();
        }
        elementData[size++] = e;
        return true;
    }

    @Override
    public E get(int index) {
        return elementData[index];
    }

    @Override
    public int size() {
        return size;
    }

    public E[] getArray() {
        return elementData;
    }

    private void grow() {
        int newCapacity = elementData.length << 1;
        elementData = Arrays.copyOf(elementData, newCapacity);
    }
}
