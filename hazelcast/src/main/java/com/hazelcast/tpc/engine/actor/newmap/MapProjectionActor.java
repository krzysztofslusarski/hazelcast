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
            project((MapProjectionMessage) msg.getMessage(), msg.getReturnMailbox());
        } else if (msg.getMessage() instanceof MapGetResultMessage) {
            projectWithGetResult((MapGetResultMessage) msg.getMessage());
        }
    }

    private void project(MapProjectionMessage message, Mailbox returnMailbox) {
        this.returnMailbox = returnMailbox;
        MapGetMessage mapGetMessage = new MapGetMessage(message.getKey());
        mapActorMailbox.offer(new Message(mapGetMessage, mailbox));
    }

    private void projectWithGetResult(MapGetResultMessage result) {
        SomeDomainObject domainObject = (SomeDomainObject) result.getValue();
        MapProjectionResultMessage resultMessage = new MapProjectionResultMessage(domainObject.getaString());
        returnMailbox.offer(new Message(resultMessage, mailbox));
        returnMailbox = null;
    }
}
