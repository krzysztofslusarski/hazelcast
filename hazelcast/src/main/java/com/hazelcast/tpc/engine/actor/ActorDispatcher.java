package com.hazelcast.tpc.engine.actor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"unchecked", "SuspiciousMethodCalls", "rawtypes"})
public class ActorDispatcher {
    private static final Map<ActorAddress, Actor> actors = new ConcurrentHashMap<>();

    public static Actor getActor(ActorAddress address) {
        return actors.get(address);
    }

    public static boolean sendMessage(ActorAddress address, Message message) {
        return actors.get(address).getMailbox().offer(message);
    }

    public static void registerNewActor(Actor actor) {
        Mailbox mailbox = actor.getMailbox();
        if (mailbox instanceof LoopLocalMailbox) {
            ((LoopLocalMailbox) mailbox).setActor(actor);
        } else if (mailbox instanceof ConcurrentMailbox) {
            ((ConcurrentMailbox) mailbox).setActor(actor);
        }
    }

    public static void registerNewActor(Actor actor, ActorAddress actorAddress) {
        registerNewActor(actor);
        actors.put(actorAddress, actor);
    }
}
