package com.hazelcast.spi.impl.reactor;


import com.hazelcast.cluster.Address;
import com.hazelcast.internal.nio.Connection;
import com.hazelcast.internal.nio.Packet;
import com.hazelcast.internal.serialization.impl.ByteArrayObjectDataInput;
import com.hazelcast.internal.util.executor.HazelcastManagedThread;
import com.hazelcast.logging.ILogger;
import com.hazelcast.table.impl.NoOp;
import com.hazelcast.table.impl.SelectByKeyOperation;
import com.hazelcast.table.impl.UpsertOp;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;

import static com.hazelcast.spi.impl.reactor.Op.RUN_CODE_DONE;
import static com.hazelcast.spi.impl.reactor.Op.RUN_CODE_FOO;
import static com.hazelcast.spi.impl.reactor.OpCodes.TABLE_NOOP;
import static com.hazelcast.spi.impl.reactor.OpCodes.TABLE_SELECT_BY_KEY;
import static com.hazelcast.spi.impl.reactor.OpCodes.TABLE_UPSERT;

public abstract class Reactor extends HazelcastManagedThread {
    protected final ReactorFrontEnd frontend;
    protected final ILogger logger;
    protected final Address thisAddress;
    protected final int port;
    protected final ChannelConfig channelConfig;
    protected final Set<Channel> channels = new CopyOnWriteArraySet<>();

    public Reactor(ReactorFrontEnd frontend, ChannelConfig channelConfig, Address thisAddress, int port, String name) {
        super(name);
        this.frontend = frontend;
        this.channelConfig = channelConfig;
        this.logger = frontend.nodeEngine.getLogger(getClass());
        this.thisAddress = thisAddress;
        this.port = port;
    }

    public Future<Channel> schedule(SocketAddress address, Connection connection) {
        System.out.println("asyncConnect connect to " + address);

        ConnectRequest request = new ConnectRequest();
        request.address = address;
        request.connection = connection;
        request.future = new CompletableFuture<>();

        schedule(request);

        return request.future;
    }

    protected abstract void schedule(ConnectRequest request);

    public static class ConnectRequest {
        public Connection connection;
        public SocketAddress address;
        public CompletableFuture<Channel> future;
    }

    @Override
    public final void executeRun() {
        try {
            setupServerSocket();
        } catch (Throwable e) {
            logger.severe(e);
            return;
        }

        try {
            eventLoop();
        } catch (Throwable e) {
            e.printStackTrace();
            logger.severe(e);
        }
    }


    protected abstract void setupServerSocket() throws Exception;

    protected abstract void eventLoop() throws Exception;

    /**
     * Is called for local requests.
     *
     * @param request
     */
    public abstract void schedule(Invocation request);

    public Collection<Channel> channels() {
        return channels;
    }

    protected void handleRemoteOp(Packet packet) {
        //System.out.println(this + " process packet: " + packet);
        try {
            byte[] bytes = packet.toByteArray();
            byte opcode = bytes[Packet.DATA_OFFSET];
            Op op = allocateOp(opcode);
            op.request.init(packet.toByteArray(), Packet.DATA_OFFSET + 1);
            handleOp(op);

            ByteBuffer byteBuffer = op.response.getByteBuffer();
            byteBuffer.flip();
            packet.channel.write(byteBuffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // local call
    protected void handleLocalOp(Invocation inv) {
        //System.out.println("request: " + request);
        try {
            byte[] data = inv.out.toByteArray();
            byte opcode = data[0];
            Op op = allocateOp(opcode);
            op.request.init(data, 1);
            handleOp(op);
            inv.future.complete(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void handleOp(Op op) {
        try {
            long callId = op.request.readLong();
            op.callId = callId;
            //System.out.println("callId: "+callId);
            int runCode = op.run();
            switch (runCode) {
                case RUN_CODE_DONE:
                    free(op);
                    return;
                case TABLE_NOOP:
                    free(op);
                    return;
                case RUN_CODE_FOO:
                    throw new RuntimeException();
                default:
                    throw new RuntimeException();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Hacky caching.
    private UpsertOp upsertOp;
    private NoOp noOp;
    private SelectByKeyOperation selectByKeyOperation;

    protected final Op allocateOp(int opcode) {
        Op op;
        switch (opcode) {
            case TABLE_UPSERT:
                if (upsertOp == null) {
                    upsertOp = new UpsertOp();
                    upsertOp.request = new ByteArrayObjectDataInput(null, frontend.ss, ByteOrder.BIG_ENDIAN);
                    upsertOp.response = new Out();
                    upsertOp.managers = frontend.managers;
                }
                op = upsertOp;
                break;
            case TABLE_SELECT_BY_KEY:
                if (selectByKeyOperation == null) {
                    selectByKeyOperation = new SelectByKeyOperation();
                    selectByKeyOperation.request = new ByteArrayObjectDataInput(null, frontend.ss, ByteOrder.BIG_ENDIAN);
                    selectByKeyOperation.response = new Out();
                    selectByKeyOperation.managers = frontend.managers;
                }
                op = selectByKeyOperation;
                break;
            case TABLE_NOOP:
                if (noOp == null) {
                    noOp = new NoOp();
                    noOp.request = new ByteArrayObjectDataInput(null, frontend.ss, ByteOrder.BIG_ENDIAN);
                    noOp.response = new Out();
                    noOp.managers = frontend.managers;
                }
                op = noOp;
                break;
            default:
                throw new RuntimeException("Unrecognized opcode:" + opcode);
        }
        op.response.init(64);
        return op;
    }

    private void free(Op op) {
        op.cleanup();


        //we should return it to the pool.
    }
}
