package com.hazelcast.tpc.engine.actor;

public class Message<E> {
    private final E message;
    private final Mailbox returnMailbox;

    public Message(E message, Mailbox returnMailbox) {
        this.message = message;
        this.returnMailbox = returnMailbox;
    }

    public E getMessage() {
        return message;
    }

    public Mailbox getReturnMailbox() {
        return returnMailbox;
    }
}
