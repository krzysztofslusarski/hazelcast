package com.hazelcast.tpc.engine.actor.newmap;

import com.hazelcast.tpc.engine.actor.LoopLocalActor;
import com.hazelcast.tpc.engine.actor.Mailbox;
import com.hazelcast.tpc.engine.actor.Message;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ALL")
public class MapActor extends LoopLocalActor {
    private final Map<Object, Object> database = new HashMap<>();
    private final Message putOkMessage = new Message(MapPutResultMessage.INSTANCE, mailbox);

    @Override
    public void process(Message msg) {
        if (msg.getMessage() instanceof MapPutMessage) {
            put((MapPutMessage) msg.getMessage(), msg.getReturnMailbox());
        } else if (msg.getMessage() instanceof MapGetMessage) {
            get((MapGetMessage) msg.getMessage(), msg.getReturnMailbox());
        }
    }

    private void get(MapGetMessage mapGetMessage, Mailbox returnMailbox) {
        MapGetResultMessage mapGetResultMessage = new MapGetResultMessage(database.get(mapGetMessage.getKey()));
        returnMailbox.offer(new Message(mapGetResultMessage, mailbox));
    }

    private void put(MapPutMessage mapPutMessage, Mailbox returnMailbox) {
        database.put(mapPutMessage.getKey(), mapPutMessage.getValue());
        returnMailbox.offer(putOkMessage);
    }
}
