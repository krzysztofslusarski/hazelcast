package com.hazelcast.tpc.engine.actor;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ConcurrentActor extends Actor {
    private final AtomicBoolean scheduled = new AtomicBoolean();

    public ConcurrentActor() {
        this(DEFAULT_MAILBOX_CAPACITY);
    }

    public ConcurrentActor(int capacity) {
        super(new ConcurrentMailbox(capacity));
    }

    @Override
    protected void newMessage() {
        if (!scheduled.get() && scheduled.compareAndSet(false, true)) {
            eventloop.execute(this);
        }
    }

    @Override
    protected void unschedule() {
        if (mailbox.isEmpty()) {
            scheduled.set(false);

            if (mailbox.isEmpty()) {
                return;
            }

            if (scheduled.compareAndSet(false, true)) {
                eventloop.execute(this);
            }
        } else {
            eventloop.execute(this);
        }
    }
}
