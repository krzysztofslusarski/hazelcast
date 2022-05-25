package com.hazelcast.tpc.engine.actor;

public class EchoActor extends LoopLocalActor {

    public EchoActor() {
    }

    @Override
    public void process(Message msg) {
        System.out.println(msg.getMessage());
    }
}
