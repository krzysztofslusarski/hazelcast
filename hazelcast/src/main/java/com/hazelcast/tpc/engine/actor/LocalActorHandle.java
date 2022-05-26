package com.hazelcast.tpc.engine.actor;

import com.hazelcast.tpc.engine.Eventloop;

public class LocalActorHandle implements ActorHandle {
    private final Actor actor;

    public LocalActorHandle(Actor actor) {
        this.actor = actor;
    }

    @Override
    public void send(Object message) {
        this.actor.send(message, null);
    }

    @Override
    public Mailbox getMailbox() {
        return actor.getMailbox();
    }

    @Override
    public Eventloop getEventloop() {
        return actor.getEventloop();
    }
}
