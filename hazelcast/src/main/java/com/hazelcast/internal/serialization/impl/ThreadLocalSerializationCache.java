package com.hazelcast.internal.serialization.impl;

import java.util.Map;

public class ThreadLocalSerializationCache {
    private static ThreadLocal<Map<Class, SerializerAdapter[]>> cache = new ThreadLocal<>();

    public static void setCache(Map<Class, SerializerAdapter[]> cache) {
        ThreadLocalSerializationCache.cache.set(cache);
    }

    public static SerializerAdapter get(Class clazz, boolean includeSchema) {
        Map<Class, SerializerAdapter[]> cache = ThreadLocalSerializationCache.cache.get();
        if (cache == null) {
            return null;
        }
        SerializerAdapter[] adapters = cache.get(clazz);
        if (adapters == null) {
            return null;
        }
        if (includeSchema) {
            return adapters[0];
        }
        return adapters[1];
    }

    public static void set(Class clazz, boolean includeSchema, SerializerAdapter serializerAdapter) {
        Map<Class, SerializerAdapter[]> cache = ThreadLocalSerializationCache.cache.get();
        if (cache == null) {
            return;
        }
        SerializerAdapter[] adapters = cache.computeIfAbsent(clazz, ignored -> new SerializerAdapter[2]);
        if (includeSchema) {
            adapters[0] = serializerAdapter;
        } else {
            adapters[1] = serializerAdapter;
        }
    }

    public static void clearCache() {
        cache.remove();
    }
}
