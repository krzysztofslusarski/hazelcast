package com.hazelcast.tpc.engine.actor;

public interface Mailbox<E> {
    boolean offer(Message<E> e);

    Message<E> poll();

    boolean isEmpty();
}
