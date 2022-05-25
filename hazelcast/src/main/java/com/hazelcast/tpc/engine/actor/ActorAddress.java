package com.hazelcast.tpc.engine.actor;

import java.util.Objects;

public class ActorAddress {
    private final String actorName;
    private final int partitionId;

    public ActorAddress(String actorName, int partitionId) {
        this.actorName = actorName;
        this.partitionId = partitionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ActorAddress that = (ActorAddress) o;
        return partitionId == that.partitionId && Objects.equals(actorName, that.actorName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actorName, partitionId);
    }
}
