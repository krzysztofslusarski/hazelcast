/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.sql.impl.exec;

import com.hazelcast.sql.impl.QueryContext;
import com.hazelcast.sql.impl.row.EmptyRowBatch;
import com.hazelcast.sql.impl.row.Row;
import com.hazelcast.sql.impl.row.RowBatch;

import java.util.Iterator;

import static com.hazelcast.sql.impl.exec.IterationResult.FETCHED_DONE;

/**
 * Upstream state.
 */
public class UpstreamState implements Iterable<Row> {
    /** Upstream operator. */
    private final Exec upstream;

    /** Iterator over the current batch. */
    private final UpstreamIterator iter;

    /** Current batch returned from the upstream. */
    private RowBatch currentBatch = EmptyRowBatch.INSTANCE;

    /** Current position. */
    private int currentBatchPos;

    /** Last returned state. */
    private IterationResult state;

    public UpstreamState(Exec upstream) {
        this.upstream = upstream;

        iter = new UpstreamIterator();
    }

    /**
     * Try advancing the upstream.
     *
     * @return {@code True} if the caller may try iteration over results; {@code false} if the caller should give
     * up execution and wait.
     */
    public boolean advance() {
        // If some data is available still, do not do anything, just return the previous result.
        if (currentBatchPos < currentBatch.getRowCount()) {
            return true;
        }

        // If the upstream is exhausted, just return "done" flag.
        if (state == FETCHED_DONE) {
            return true;
        }

        // Otherwise poll the upstream.
        state = upstream.advance();

        switch (state) {
            case FETCHED_DONE:
            case FETCHED:
                currentBatch = upstream.currentBatch();
                assert currentBatch != null;

                currentBatchPos = 0;

                return true;

            case WAIT:
                currentBatch = EmptyRowBatch.INSTANCE;
                currentBatchPos = 0;

                return false;

            default:
                throw new IllegalStateException("Should not reach this.");
        }
    }

    public void setup(QueryContext ctx) {
        upstream.setup(ctx);
    }

    public RowBatch getCurrentBatch() {
        return currentBatch;
    }

    /**
     * @return {@code True} if no more results will appear in future.
     */
    public boolean isDone() {
        return state == FETCHED_DONE && !iter.hasNext();
    }

    public boolean canReset() {
        return upstream.canReset();
    }

    public void reset() {
        upstream.reset();

        currentBatch = EmptyRowBatch.INSTANCE;
        currentBatchPos = 0;

        state = null;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<Row> iterator() {
        return iter;
    }

    /**
     * Return next row in the current batch if it exists.
     *
     * @return Next row or {@code null}.
     */
    public Row nextIfExists() {
        return iter.hasNext() ? iter.next() : null;
    }

    /**
     * Iterator over current upstream batch.
     */
    private class UpstreamIterator implements Iterator<Row> {
        @Override
        public boolean hasNext() {
            return currentBatchPos < currentBatch.getRowCount();
        }

        @Override
        public Row next() {
            return currentBatch.getRow(currentBatchPos++);
        }
    }
}
