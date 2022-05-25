package com.hazelcast.tpc.engine.actor;

import org.jctools.queues.MpscArrayQueue;

class ConcurrentMailbox<E> implements Mailbox<E> {
    private final MpscArrayQueue<Message<E>> queue;
    private volatile Actor actor;

    ConcurrentMailbox(int capacity) {
        this.queue = new MpscArrayQueue<>(capacity);
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

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void setActor(Actor actor) {
        this.actor = actor;
    }
}
