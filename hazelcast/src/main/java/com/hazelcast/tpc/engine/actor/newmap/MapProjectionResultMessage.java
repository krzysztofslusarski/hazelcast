package com.hazelcast.tpc.engine.actor.newmap;

public class MapProjectionResultMessage {
    private final Object result;

    public MapProjectionResultMessage(Object result) {
        this.result = result;
    }

    public Object getResult() {
        return result;
    }
}
