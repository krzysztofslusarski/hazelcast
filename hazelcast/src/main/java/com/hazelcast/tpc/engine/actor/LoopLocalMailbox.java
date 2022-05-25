package com.hazelcast.tpc.engine.actor;

import com.hazelcast.tpc.util.CircularQueue;

class LoopLocalMailbox<E> implements Mailbox<E> {
    private final CircularQueue<Message<E>> queue;
    private Actor actor;

    LoopLocalMailbox(int capacity) {
        this.queue = new CircularQueue<>(capacity);
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    @Override
    public boolean offer(Message<E> e) {
        boolean offer = queue.offer(e);
        if (offer) {
            actor.newMessage();
        } else {
            throw new IllegalStateException("TOOD");
        }
        return offer;
    }

    @Override
    public Message<E> poll() {
        return queue.poll();
    }

    public void setActor(Actor actor) {
        this.actor = actor;
    }
}
