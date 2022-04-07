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

package com.hazelcast.internal.nio;

import java.util.concurrent.ConcurrentLinkedQueue;

public final class PayloadFactory {
    private static final int QUEUES_COUNT = 11;
    private static final int CACHED_PAYLOAD_MIN_BUFFER_SIZE = 2048;
    private static final int CACHED_PAYLOAD_MIN_SIZE = CACHED_PAYLOAD_MIN_BUFFER_SIZE >> 1;
    private static final int CACHED_PAYLOAD_MAX_SIZE = CACHED_PAYLOAD_MIN_BUFFER_SIZE << (QUEUES_COUNT - 1);
    private static final ConcurrentLinkedQueue<ReusablePayload>[] REUSABLE_QUEUES = new ConcurrentLinkedQueue[QUEUES_COUNT];

    private PayloadFactory() {
    }

    static {
        for (int i = 0; i < QUEUES_COUNT; i++) {
            REUSABLE_QUEUES[i] = new ConcurrentLinkedQueue<>();
        }
    }

    public static Payload getPayload(int size) {
        int queueIndex = findQueueIndex(size);
        if (queueIndex == -1) {
            return new NotReusablePayload(size);
        }
        ConcurrentLinkedQueue<ReusablePayload> queue = REUSABLE_QUEUES[queueIndex];
        ReusablePayload reusablePayload = queue.poll();
        if (reusablePayload == null) {
            reusablePayload = new ReusablePayload(CACHED_PAYLOAD_MIN_BUFFER_SIZE << queueIndex);
        }
        reusablePayload.setCurrentSize(size);
        return reusablePayload;
    }

    public static void reclaim(Payload payload) {
        if (!(payload instanceof ReusablePayload)) {
            return;
        }
        ReusablePayload reusablePayload = (ReusablePayload) payload;
        int queueIndex = findQueueIndex(payload.getCurrentSize());

        assert queueIndex != -1;

        REUSABLE_QUEUES[queueIndex].offer(reusablePayload);
    }

    static int findQueueIndex(int size) {
        if (size < CACHED_PAYLOAD_MIN_SIZE || size > CACHED_PAYLOAD_MAX_SIZE) {
            return -1;
        }

        int currentSize = CACHED_PAYLOAD_MIN_BUFFER_SIZE;
        for (int i = 0; i < QUEUES_COUNT; i++) {
            if (size <= currentSize) {
                return i;
            }
            currentSize <<= 1;
        }
        assert false;
        return -1;
    }

}
