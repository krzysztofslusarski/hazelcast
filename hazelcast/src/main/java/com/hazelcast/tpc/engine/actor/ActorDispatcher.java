package com.hazelcast.tpc.engine.actor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("ALL")
public class ActorDispatcher {
    private static final Map<ActorAddress, Actor> addressToActor = new ConcurrentHashMap<>();
    private static final Map<UUID, Actor> idToActor = new ConcurrentHashMap<>();

    public static ActorHandle getActor(ActorAddress address) {
        return addressToActor.get(address).getHandle();
    }

    public static ActorHandle getActor(UUID uuid) {
        return idToActor.get(uuid).getHandle();
    }

    public static boolean sendMessage(ActorAddress address, Message message) {
        return addressToActor.get(address).getMailbox().offer(message);
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
        addressToActor.put(actorAddress, actor);
    }
}
