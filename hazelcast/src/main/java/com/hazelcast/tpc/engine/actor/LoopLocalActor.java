package com.hazelcast.tpc.engine.actor;

public abstract class LoopLocalActor extends Actor {
    private boolean localScheduled;

    public LoopLocalActor() {
        this(DEFAULT_MAILBOX_CAPACITY);
    }

    public LoopLocalActor(int capacity) {
        super(new LoopLocalMailbox(capacity));
    }

    @Override
    protected void newMessage() {
        if (!localScheduled) {
            localScheduled = true;
            eventloop.execute(this);
        }
    }

    @Override
    protected void unschedule() {
        if (mailbox.isEmpty()) {
            localScheduled = false;
        } else {
            eventloop.execute(this);
        }
    }
}

