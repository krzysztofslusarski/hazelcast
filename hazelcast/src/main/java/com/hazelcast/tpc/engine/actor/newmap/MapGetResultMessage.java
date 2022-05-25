package com.hazelcast.tpc.engine.actor.newmap;

public class MapGetResultMessage {
    private final Object value;

    public MapGetResultMessage(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
