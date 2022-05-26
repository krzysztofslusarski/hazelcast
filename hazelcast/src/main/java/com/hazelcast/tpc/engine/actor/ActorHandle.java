package com.hazelcast.tpc.engine.actor;

import com.hazelcast.tpc.engine.Eventloop;

@SuppressWarnings("ALL")
public interface ActorHandle {
    void send(Object message);

    Mailbox getMailbox();

    Eventloop getEventloop();
}
