package com.hazelcast.tpc.requestservice;

import com.hazelcast.internal.util.HashUtil;
import com.hazelcast.internal.util.concurrent.MPSCQueue;
import com.hazelcast.tpc.engine.frame.Frame;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import static com.hazelcast.internal.util.HashUtil.hashToIndex;
import static com.hazelcast.tpc.engine.frame.Frame.OFFSET_RES_CALL_ID;

class ResponseHandler implements Consumer<Frame> {

    private final ResponseThread[] threads;
    private final int threadCount;
    private final boolean spin;
    private final ConcurrentMap<SocketAddress, RequestService.Requests> requestRegistry;

    ResponseHandler(int threadCount,
                    boolean spin,
                    ConcurrentMap<SocketAddress, RequestService.Requests> requestRegistry) {
        this.spin = spin;
        this.threadCount = threadCount;
        this.threads = new ResponseThread[threadCount];
        this.requestRegistry = requestRegistry;
        for (int k = 0; k < threadCount; k++) {
            this.threads[k] = new ResponseThread(k);
        }
    }

    // TODO: We can simplify this by attaching the requests for a member, directly to that
    // channel so we don't need to do a requests lookup.
    public void handleResponse(Frame response) {
        if (response.next != null) {
            int index = threadCount == 0
                    ? 0
                    : hashToIndex(response.getLong(OFFSET_RES_CALL_ID), threadCount);
            threads[index].queue.add(response);
            return;
        }

        try {
            RequestService.Requests requests = requestRegistry.get(response.socket.getRemoteAddress());
            if (requests == null) {
                System.out.println("Dropping response " + response + ", requests not found");
                response.release();
            } else {
                requests.complete();

                long callId = response.getLong(OFFSET_RES_CALL_ID);
                //System.out.println("response with callId:"+callId +" frame: "+response);

                Frame request = requests.map.remove(callId);
                if (request == null) {
                    System.out.println("Dropping response " + response + ", invocation with id " + callId + " not found");
                } else {
                    CompletableFuture future = request.future;
                    future.complete(response);
                    request.release();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void start() {
        for (ResponseThread t : threads) {
            t.start();
        }
    }

    void shutdown() {
        for (ResponseThread t : threads) {
            t.shutdown();
        }
    }

    @Override
    public void accept(Frame response) {
        if (response.next != null) {
            int index = threadCount == 0
                    ? 0
                    : HashUtil.hashToIndex(response.getLong(OFFSET_RES_CALL_ID), threadCount);
            threads[index].queue.add(response);
            return;
        }

        try {
            RequestService.Requests requests = requestRegistry.get(response.socket.getRemoteAddress());
            if (requests == null) {
                System.out.println("Dropping response " + response + ", requests not found");
                response.release();
            } else {
                requests.complete();

                long callId = response.getLong(OFFSET_RES_CALL_ID);
                //System.out.println("response with callId:"+callId +" frame: "+response);

                Frame request = requests.map.remove(callId);
                if (request == null) {
                    System.out.println("Dropping response " + response + ", invocation with id " + callId + " not found");
                } else {
                    CompletableFuture future = request.future;
                    future.complete(response);
                    request.release();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ResponseThread extends Thread {

        private final MPSCQueue<Frame> queue;
        private volatile boolean shuttingdown = false;

        private ResponseThread(int index) {
            super("ResponseThread-" + index);
            this.queue = new MPSCQueue<>(this, null);
        }

        @Override
        public void run() {
            try {
                while (!shuttingdown) {
                    Frame frame;
                    if (spin) {
                        do {
                            frame = queue.poll();
                        } while (frame == null);
                    } else {
                        frame = queue.take();
                    }

                    do {
                        Frame next = frame.next;
                        frame.next = null;
                        accept(frame);
                        frame = next;
                    } while (frame != null);
                }
            } catch (InterruptedException e) {
                System.out.println("ResponseThread stopping due to interrupt");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void shutdown() {
            shuttingdown = true;
            interrupt();
        }
    }
}
