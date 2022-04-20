package com.hazelcast.profiler;

import com.hazelcast.internal.util.tracing.TracingContext;
import com.hazelcast.jet.impl.util.IOUtil;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class AsyncProfilerTracingContext extends TracingContext {
    private static final String FILE_LIBASYNC_PROFILER_SO = "/tmp/libasyncProfiler.so";
    private static final String CP_LIBASYNC_PROFILER_SO = "/libasyncProfiler.so";
    private static final String EVENT = "wall";
    private final AsyncProfiler asyncProfiler;
    private final ThreadLocal<Long> ecid = new ThreadLocal<>();

    public AsyncProfilerTracingContext() throws IOException {
        try (InputStream inputStream = this.getClass().getResourceAsStream(CP_LIBASYNC_PROFILER_SO);
             OutputStream outputStream = new FileOutputStream(FILE_LIBASYNC_PROFILER_SO)
        ) {
            IOUtil.copyStream(inputStream, outputStream);
        }
        asyncProfiler = AsyncProfiler.getInstance(FILE_LIBASYNC_PROFILER_SO);
        asyncProfiler.execute("start,jfr,event=" + EVENT + ",file=prof.jfr");
//        asyncProfiler.execute("start,jfr,event=" + EVENT + ",file=prof.jfr,interval=1000000");
    }

    @Override
    public void setCorrelationId(Long correlationId) {
        if (correlationId == null || correlationId == 0) {
            asyncProfiler.clearContextId();
            ecid.remove();
        } else {
            asyncProfiler.setContextId(correlationId);
            ecid.set(correlationId);
        }
    }

    @Override
    public void generateAndSetCorrelationId() {
        UUID uuid = UUID.randomUUID();
        long ecid = uuid.getLeastSignificantBits() ^ uuid.getMostSignificantBits();
        setCorrelationId(ecid);
    }

    @Override
    public void clearCorrelationId() {
        asyncProfiler.clearContextId();
        ecid.remove();
    }

    @Override
    public Long getCorrelationId() {
        return ecid.get();
    }

    @Override
    public void close() throws IOException {
        asyncProfiler.execute("stop,jfr,event=" + EVENT + ",file=prof.jfr");
        //        asyncProfiler.execute("stop,jfr,event=" + EVENT + ",file=prof.jfr,interval=1000000");
    }
}
