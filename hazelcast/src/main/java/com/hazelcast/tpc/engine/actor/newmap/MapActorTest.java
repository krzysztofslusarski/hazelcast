package com.hazelcast.tpc.engine.actor.newmap;

import com.hazelcast.internal.util.ThreadAffinity;
import com.hazelcast.tpc.engine.Eventloop;
import com.hazelcast.tpc.engine.actor.ActorAddress;
import com.hazelcast.tpc.engine.actor.ActorDispatcher;
import com.hazelcast.tpc.engine.actor.Message;
import com.hazelcast.tpc.engine.nio.NioEventloop;

import java.io.IOException;

@SuppressWarnings("ALL")
public class MapActorTest {
    public static final int PARTITION_COUNT = 10;

    public static void main(String[] args) throws IOException {
        for (int i = 0; i < PARTITION_COUNT; i++) {
            Eventloop eventloop = new NioEventloop();
            eventloop.setThreadAffinity(new ThreadAffinity(i + ""));
            eventloop.start();

            MapActor mapActor = new MapActor();
            mapActor.activate(eventloop);
            ActorDispatcher.registerNewActor(mapActor, new ActorAddress("MapActor", i));
        }

        Runner runner = new Runner();
        ActorDispatcher.registerNewActor(runner);
        runner.getMailbox().offer(new Message(null, null));
        System.in.read();
    }

}
