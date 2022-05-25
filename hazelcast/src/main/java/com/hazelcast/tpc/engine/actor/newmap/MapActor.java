package com.hazelcast.tpc.engine.actor.newmap;

import com.hazelcast.tpc.engine.actor.LoopLocalActor;
import com.hazelcast.tpc.engine.actor.Message;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"rawtypes", "unchecked"})
public class MapActor extends LoopLocalActor {
    private final Map<Object, Object> database = new HashMap<>();
    private final Message putOkMessage = new Message(MapPutResultMessage.INSTANCE, mailbox);

    @Override
    public void process(Message msg) {
        if (msg.getMessage() instanceof MapPutMessage) {
            MapPutMessage mapPutMessage = (MapPutMessage) msg.getMessage();
            database.put(mapPutMessage.getKey(), mapPutMessage.getValue());
            msg.getReturnMailbox().offer(putOkMessage);
        } else if (msg.getMessage() instanceof MapGetMessage) {
            MapGetMessage mapGetMessage = (MapGetMessage) msg.getMessage();
            MapGetResultMessage mapGetResultMessage = new MapGetResultMessage(database.get(mapGetMessage.getKey()));
            msg.getReturnMailbox().offer(new Message(mapGetResultMessage, mailbox));
        }
    }
}
