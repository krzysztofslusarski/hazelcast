package com.hazelcast.tpc.engine.actor.newmap;

public class MapGetMessage {
    private final Object key;

    public MapGetMessage(Object key) {
        this.key = key;
    }

    public Object getKey() {
        return key;
    }
}
