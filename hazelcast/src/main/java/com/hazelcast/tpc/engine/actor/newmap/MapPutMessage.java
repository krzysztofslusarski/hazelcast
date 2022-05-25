package com.hazelcast.tpc.engine.actor.newmap;

public class MapPutMessage {
    private final Object key;
    private final Object value;

    public MapPutMessage(Object key, Object value) {
        this.key = key;
        this.value = value;
    }

    public Object getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }
}
