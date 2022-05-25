package com.hazelcast.tpc.engine.actor.newmap;

import com.hazelcast.tpc.engine.actor.Actor;
import com.hazelcast.tpc.engine.actor.ActorAddress;
import com.hazelcast.tpc.engine.actor.ActorDispatcher;
import com.hazelcast.tpc.engine.actor.ConcurrentActor;
import com.hazelcast.tpc.engine.actor.Message;

public class Runner extends ConcurrentActor {
    private MapProjectionActor mapProjectionActor;
    private Actor mapActor;

    public Runner() {
        mapActor = ActorDispatcher.getActor(new ActorAddress("MapActor", 1));
        mapProjectionActor = new MapProjectionActor(mapActor.getMailbox());
        mapProjectionActor.activate(mapActor.getEventloop());
        ActorDispatcher.registerNewActor(mapProjectionActor);
        activate(mapActor.getEventloop());
    }

    @Override
    public void process(Message msg) {
        if (msg.getMessage() == null) {
            MapPutMessage mapPutMessage = new MapPutMessage(1, new SomeDomainObject(1, "haha"));
            mapActor.getMailbox().offer(new Message(mapPutMessage, mailbox));
        } else if (msg.getMessage() instanceof MapPutResultMessage) {
            MapProjectionMessage mapProjectionMessage = new MapProjectionMessage(1);
            mapProjectionActor.getMailbox().offer(new Message(mapProjectionMessage, mailbox));
        } else if (msg.getMessage() instanceof MapProjectionResultMessage) {
            MapProjectionResultMessage message = (MapProjectionResultMessage) msg.getMessage();
            System.out.println(message.getResult());
        }
    }
}
