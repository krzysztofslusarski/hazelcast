package com.hazelcast.tpc.engine.actor.newmap;

public class MapProjectionMessage {
    private final Object key;

    public MapProjectionMessage(Object key) {
        this.key = key;
    }

    public Object getKey() {
        return key;
    }
}
