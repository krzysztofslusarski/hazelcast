package com.hazelcast.tpc.engine.actor.newmap;

import com.hazelcast.tpc.engine.Eventloop;
import com.hazelcast.tpc.engine.actor.ActorAddress;
import com.hazelcast.tpc.engine.actor.ActorDispatcher;
import com.hazelcast.tpc.engine.actor.ActorHandle;
import com.hazelcast.tpc.engine.actor.ConcurrentActor;
import com.hazelcast.tpc.engine.actor.Mailbox;
import com.hazelcast.tpc.engine.actor.Message;


@SuppressWarnings("ALL")
public class Runner extends ConcurrentActor {
    private Mailbox mapProjectionMailbox;
    private Mailbox mapActorMailbox;

    public Runner() {
        ActorHandle mapActor = ActorDispatcher.getActor(new ActorAddress("MapActor", 1));
        Eventloop eventloop = mapActor.getEventloop();

        MapProjectionActor mapProjectionActor = new MapProjectionActor(mapActor.getMailbox());
        ActorDispatcher.registerNewActor(mapProjectionActor);

        mapProjectionActor.activate(eventloop);
        activate(eventloop);

        mapProjectionMailbox = mapProjectionActor.getMailbox();
        mapActorMailbox = mapActor.getMailbox();
    }

    @Override
    public void process(Message msg) {
        if (msg.getMessage() == null) {
            start();
        } else if (msg.getMessage() instanceof MapPutResultMessage) {
            processAfterPut();
        } else if (msg.getMessage() instanceof MapProjectionResultMessage) {
            processAfterProjection(msg);
        }
    }

    private void start() {
        MapPutMessage mapPutMessage = new MapPutMessage(1, new SomeDomainObject(1, "haha"));
        mapActorMailbox.offer(new Message(mapPutMessage, mailbox));
    }

    private void processAfterPut() {
        MapProjectionMessage mapProjectionMessage = new MapProjectionMessage(1);
        mapProjectionMailbox.offer(new Message(mapProjectionMessage, mailbox));
    }

    private void processAfterProjection(Message msg) {
        MapProjectionResultMessage message = (MapProjectionResultMessage) msg.getMessage();
        System.out.println(message.getResult());
    }
}
