package com.hazelcast.internal.tpc.iobuffer;

import static com.hazelcast.internal.tpc.iobuffer.ThreadLocalIOBufferAllocator.INITIAL_POOL_SIZE;

public abstract class IOBufferAllocatorFactory {
    private static final ConcurrentIOBufferAllocator CONCURRENT_IO_BUFFER_ALLOCATOR = new ConcurrentIOBufferAllocator();

    private IOBufferAllocatorFactory() {
    }

    public static IOBufferAllocator<ThreadLocalIOBuffer> createGrowingThreadLocal() {
        return new ThreadLocalIOBufferAllocator(true, INITIAL_POOL_SIZE, null);
    }

    public static IOBufferAllocator<ThreadLocalIOBuffer> createNotGrowingThreadLocal(
            int maxPoolSize,
            ConcurrentIOBufferAllocator concurrentAllocator
    ) {
        return new ThreadLocalIOBufferAllocator(false, maxPoolSize, concurrentAllocator);
    }

    public static IOBufferAllocator<ThreadLocalIOBuffer> createConcurrentAllocator() {
        return CONCURRENT_IO_BUFFER_ALLOCATOR;
    }
}
