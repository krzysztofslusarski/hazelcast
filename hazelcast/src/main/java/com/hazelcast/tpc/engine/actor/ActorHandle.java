package com.hazelcast.tpc.engine.actor;

import com.hazelcast.tpc.engine.Eventloop;

public interface ActorHandle {

    void send(Object message);

    Mailbox getMailbox();


    Eventloop getEventloop();
}
