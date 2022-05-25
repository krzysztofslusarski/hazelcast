package com.hazelcast.tpc.engine.actor;

import com.hazelcast.tpc.engine.Eventloop;
import com.hazelcast.tpc.engine.EventloopTask;

public abstract class Actor implements EventloopTask {
    public final static int DEFAULT_MAILBOX_CAPACITY = 512;

    protected final Mailbox mailbox;
    protected Eventloop eventloop;

    private final LocalActorHandle handle = new LocalActorHandle(this);

    public LocalActorHandle getHandle() {
        return handle;
    }

    public Eventloop getEventloop() {
        return eventloop;
    }

    public void activate(Eventloop eventloop) {
        this.eventloop = eventloop;
    }

    public Actor(Mailbox mailbox) {
        this.mailbox = mailbox;
    }

    protected abstract void newMessage();

    void send(Object msg, Mailbox returnMailbox) {
        //todo: we need to deal with overload.
        mailbox.offer(new Message(msg, returnMailbox));
        newMessage();
    }

    @Override
    public void run() throws Exception {
        Message msg = mailbox.poll();
        if (msg != null) {
            try {
                process(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        unschedule();
    }

    protected abstract void unschedule();

    public boolean sameEventLoop(Actor anotherActor) {
        return eventloop != null && eventloop == anotherActor.eventloop;
    }

    public Mailbox getMailbox() {
        return mailbox;
    }

    public abstract void process(Message msg);
}
