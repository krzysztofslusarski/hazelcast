package com.hazelcast.tpc.engine.actor.newmap;

import com.hazelcast.tpc.engine.actor.LoopLocalActor;
import com.hazelcast.tpc.engine.actor.Mailbox;
import com.hazelcast.tpc.engine.actor.Message;

@SuppressWarnings("ALL")
public class MapProjectionActor extends LoopLocalActor {
    private final Mailbox mapActorMailbox;
    private Mailbox returnMailbox;

    public MapProjectionActor(Mailbox mapActorMailbox) {
        this.mapActorMailbox = mapActorMailbox;
    }

    @Override
    public void process(Message msg) {
        if (msg.getMessage() instanceof MapProjectionMessage) {
            MapProjectionMessage message = (MapProjectionMessage) msg.getMessage();
            returnMailbox = msg.getReturnMailbox();
            MapGetMessage mapGetMessage = new MapGetMessage(message.getKey());
            mapActorMailbox.offer(new Message(mapGetMessage, mailbox));
        } else if (msg.getMessage() instanceof MapGetResultMessage) {
            MapGetResultMessage result = (MapGetResultMessage) msg.getMessage();
            SomeDomainObject domainObject = (SomeDomainObject) result.getValue();
            MapProjectionResultMessage resultMessage = new MapProjectionResultMessage(domainObject.getaString());
            returnMailbox.offer(new Message(resultMessage, mailbox));
            returnMailbox = null;
        }
    }
}
