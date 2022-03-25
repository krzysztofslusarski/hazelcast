package com.hazelcast.spi.impl.reactor.nio;

import com.hazelcast.cluster.Address;
import com.hazelcast.instance.EndpointQualifier;
import com.hazelcast.internal.nio.Packet;
import com.hazelcast.internal.nio.PacketIOHelper;
import com.hazelcast.internal.serialization.InternalSerializationService;
import com.hazelcast.internal.serialization.impl.ByteArrayObjectDataInput;
import com.hazelcast.internal.server.ServerConnectionManager;
import com.hazelcast.internal.server.tcp.TcpServerConnection;
import com.hazelcast.internal.util.HashUtil;
import com.hazelcast.internal.util.ThreadAffinity;
import com.hazelcast.logging.ILogger;
import com.hazelcast.spi.impl.NodeEngineImpl;
import com.hazelcast.spi.impl.reactor.Channel;
import com.hazelcast.spi.impl.reactor.Invocation;
import com.hazelcast.spi.impl.reactor.Managers;
import com.hazelcast.spi.impl.reactor.Request;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.util.concurrent.TimeUnit.SECONDS;

public class NioReactorFrontEnd implements com.hazelcast.spi.impl.reactor.ReactorFrontEnd {

    private final NodeEngineImpl nodeEngine;
    public final InternalSerializationService ss;
    public final ILogger logger;
    private final Address thisAddress;
    private final ThreadAffinity threadAffinity;
    private volatile ServerConnectionManager connectionManager;
    public volatile boolean shuttingdown = false;
    private final NioReactor[] reactors;
    public final Managers managers = new Managers();
    private final ConcurrentMap<Address, ConnectionInvocations> invocationsPerMember = new ConcurrentHashMap<>();

    public NioReactorFrontEnd(NodeEngineImpl nodeEngine) {
        this.nodeEngine = nodeEngine;
        this.logger = nodeEngine.getLogger(NioReactorFrontEnd.class);
        this.ss = (InternalSerializationService) nodeEngine.getSerializationService();

        int reactorTreadCount = Integer.parseInt(System.getProperty("reactor.threadcount", "" + Runtime.getRuntime().availableProcessors()));
        logger.info("Reactor threadcount:" + reactorTreadCount);
        this.reactors = new NioReactor[reactorTreadCount];
        this.thisAddress = nodeEngine.getThisAddress();
        this.threadAffinity = ThreadAffinity.newSystemThreadAffinity("reactor-threadaffinity");
        for (int cpu = 0; cpu < reactors.length; cpu++) {
            int port = toPort(thisAddress, cpu);
            reactors[cpu] = new NioReactor(this, thisAddress, port);
            reactors[cpu].setThreadAffinity(threadAffinity);
        }
    }

    public int toPort(Address address, int cpu) {
        return (address.getPort() - 5701) * 100 + 11000 + cpu;
    }

    public int partitionIdToCpu(int partitionId) {
        return HashUtil.hashToIndex(partitionId, reactors.length);
    }

    public void start() {
        logger.finest("Starting ReactorServicee");

        for (NioReactor t : reactors) {
            t.start();
        }
    }

    public void shutdown() {
        shuttingdown = true;
    }


    @Override
    public CompletableFuture invoke(Request request) {
        try {
            int partitionId = request.partitionId;

            if (partitionId >= 0) {
                Address targetAddress = nodeEngine.getPartitionService().getPartitionOwner(partitionId);
                ConnectionInvocations invocations = getConnectionInvocations(targetAddress);
                Invocation invocation = new Invocation();
                invocation.callId = invocations.counter.incrementAndGet();
                request.out.writeLong(Request.OFFSET_CALL_ID, invocation.callId);
                invocation.request = request;
                request.invocation = invocation;
                invocations.map.put(invocation.callId, invocation);

                if (targetAddress.equals(thisAddress)) {
                    //System.out.println("local invoke");
                    reactors[partitionIdToCpu(partitionId)].enqueue(request);
                } else {
                    //System.out.println("remove invoke");
                    if (connectionManager == null) {
                        connectionManager = nodeEngine.getNode().getServer().getConnectionManager(EndpointQualifier.MEMBER);
                    }

                    TcpServerConnection connection = getConnection(targetAddress);
                    Channel channel = null;
                    for (int k = 0; k < 10; k++) {
                        Channel[] channels = (Channel[]) connection.junk;
                        if (channels != null) {
                            channel = channels[partitionIdToCpu(partitionId)];
                            break;
                        }

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }

                    if (channel == null) {
                        throw new RuntimeException("Could not connect to " + targetAddress + " partitionId:" + partitionId);
                    }

                    Packet packet = request.toPacket();
                    ByteBuffer buffer = ByteBuffer.allocate(packet.totalSize() + 30);
                    new PacketIOHelper().writeTo(packet, buffer);
                    buffer.flip();
                    channel.writeAndFlush(buffer);
                }

                return invocation.completableFuture;
            } else {
                throw new RuntimeException("Negative partition id not supported:" + partitionId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private TcpServerConnection getConnection(Address targetAddress) {
        TcpServerConnection connection = (TcpServerConnection) connectionManager.get(targetAddress);
        if (connection == null) {
            connectionManager.getOrConnect(targetAddress);
            try {
                if (!connectionManager.blockOnConnect(thisAddress, SECONDS.toMillis(10), 0)) {
                    throw new RuntimeException();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if (connection.junk == null) {
            synchronized (connection) {
                if (connection.junk == null) {
                    Channel[] channels = new Channel[reactors.length];
                    Address remoteAddress = connection.getRemoteAddress();

                    for (int cpu = 0; cpu < channels.length; cpu++) {
                        SocketAddress socketAddress = new InetSocketAddress(remoteAddress.getHost(), toPort(remoteAddress, cpu));
                        Future<Channel> f = reactors[cpu].enqueue(socketAddress, connection);
                        try {
                            channels[cpu] = f.get();
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to connect to :" + socketAddress, e);
                        }
                        //todo: assignment of the socket to the channels.
                    }

                    connection.junk = channels;
                }
            }
        }
        return connection;
    }

    public ConnectionInvocations getConnectionInvocations(Address address) {
        ConnectionInvocations invocations = invocationsPerMember.get(address);
        if (invocations != null) {
            return invocations;
        }

        ConnectionInvocations newInvocations = new ConnectionInvocations(address);
        ConnectionInvocations foundInvocations = invocationsPerMember.putIfAbsent(address, newInvocations);
        return foundInvocations == null ? newInvocations : foundInvocations;
    }

    private class ConnectionInvocations {
        private final Address target;
        private final ConcurrentMap<Long, Invocation> map = new ConcurrentHashMap<>();
        private final AtomicLong counter = new AtomicLong(500);

        public ConnectionInvocations(Address target) {
            this.target = target;
        }
    }

    public void handleResponse(Packet packet) {
        try {
            Address remoteAddress = packet.getConn().getRemoteAddress();
            ConnectionInvocations targetInvocations = invocationsPerMember.get(remoteAddress);
            if (targetInvocations == null) {
                System.out.println("Dropping response " + packet + ", targetInvocations not found");
                return;
            }

            ByteArrayObjectDataInput in = new ByteArrayObjectDataInput(packet.toByteArray(), ss, BIG_ENDIAN);

            long callId = in.readLong();
            Invocation invocation = targetInvocations.map.get(callId);
            if (invocation == null) {
                System.out.println("Dropping response " + packet + ", invocation not found");
            } else {
                invocation.completableFuture.complete(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


//    //todo: add option to bypass offloading to thread for thread per core versionn
//    @Override
//    public void accept(Packet packet) {
//        if (packet.isFlagRaised(FLAG_OP_RESPONSE)) {
//            Address remoteAddress = packet.getConn().getRemoteAddress();
//            TargetInvocations targetInvocations = invocationsPerMember.get(remoteAddress);
//            if (targetInvocations == null) {
//                System.out.println("Dropping response " + packet + ", targetInvocations not found");
//                return;
//            }
//
//            long callId = 0;
//            Invocation invocation = targetInvocations.map.get(callId);
//            if (invocation == null) {
//                System.out.println("Dropping response " + packet + ", invocation not found");
//                invocation.completableFuture.complete(null);
//            }
//        } else {
//            int index = HashUtil.hashToIndex(packet.getPartitionHash(), reactors.length);
//            reactors[index].enqueue(packet);
//        }
//    }

}
