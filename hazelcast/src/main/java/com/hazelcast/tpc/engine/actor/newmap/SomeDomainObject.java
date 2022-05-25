package com.hazelcast.tpc.engine.actor.newmap;

public class SomeDomainObject {
    private final int anInt;
    private final String aString;

    public SomeDomainObject(int anInt, String aString) {
        this.anInt = anInt;
        this.aString = aString;
    }

    public int getAnInt() {
        return anInt;
    }

    public String getaString() {
        return aString;
    }
}
