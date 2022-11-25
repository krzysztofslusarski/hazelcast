package com.hazelcast.internal.tpc.iobuffer;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.hazelcast.internal.nio.Bits.INT_SIZE_IN_BYTES;
import static com.hazelcast.internal.nio.Bits.LONG_SIZE_IN_BYTES;
import static com.hazelcast.internal.nio.Bits.SHORT_SIZE_IN_BYTES;
import static com.hazelcast.internal.tpc.iobuffer.TpcIOBufferAllocator.BUFFER_SIZE;

class TpcIOBuffer implements IOBuffer {
    private final TpcIOBufferAllocator allocator;

    ByteBuffer[] chunks;

    /**
     * Position of a last chunk in.
     */
    private int chunksPos;

    /**
     * Position of a last byte in a buffer.
     */
    private int pos;

    private int totalCapacity;

    TpcIOBuffer(TpcIOBufferAllocator allocator, int minSize) {
        this.allocator = allocator;
        this.chunks = new ByteBuffer[((minSize - 1)/ BUFFER_SIZE) + 1];
        for (int i = 0; i < chunks.length; i++) {
            addChunk(allocator.getNextBuffer());
        }
    }

    @Override
    public void release() {
        allocator.free(this);
    }

    @Override
    public int position() {
        return pos;
    }

    @Override
    public void clear() {
        for (int i = 0; i < chunksPos; i++) {
            chunks[i].clear();
        }
        pos = 0;
    }

    @Override
    public void flip() {
        throw new NotImplementedYetException();
    }

    @Override
    public byte getByte(int pos) {
        int chunk = pos / BUFFER_SIZE;
        int posInChunk = pos % BUFFER_SIZE;
        return chunks[chunk].get(posInChunk);
    }

    @Override
    public void writeByte(byte src) {
        ensureRemaining(1);
        writeByteUnsafe(src);
    }

    private void writeByteUnsafe(byte src) {
        int chunk = pos / BUFFER_SIZE;
        chunks[chunk].put(src);
        pos++;
    }

    @Override
    public void writeBytes(byte[] src) {
        ensureRemaining(src.length);
        int arrayPos = 0;
        int chunk = pos / BUFFER_SIZE;
        int toWriteRemaining = src.length - arrayPos;
        while (toWriteRemaining > 0) {
            int toWriteInCurrentChunk = Math.min(chunks[chunk].remaining(), toWriteRemaining);;
            chunks[chunk].put(src, arrayPos, toWriteInCurrentChunk);
            arrayPos += toWriteInCurrentChunk;
            toWriteRemaining -= toWriteInCurrentChunk;
            pos += toWriteInCurrentChunk;
        }
    }

    @Override
    public void writeShortL(short v) {
        ensureRemaining(SHORT_SIZE_IN_BYTES);
        writeByteUnsafe((byte) ((v) & 0xFF));
        writeByteUnsafe((byte) ((v >>> 8) & 0xFF));
    }

    @Override
    public int getInt(int index) {
        int firstChunk = index / BUFFER_SIZE;
        int lastChunk = (index + INT_SIZE_IN_BYTES - 1) / BUFFER_SIZE;
        if (firstChunk == lastChunk) {
            int ret = chunks[firstChunk].getInt(index);
            return ret;
        }
        int result = 0;
        for (int i = 0; i < INT_SIZE_IN_BYTES; i++) {
            result = result << 8;
            result += chunks[(index + i) / BUFFER_SIZE].get((index + i) % BUFFER_SIZE);
        }
        return result;
    }

    @Override
    public void writeInt(int value) {
        if (BUFFER_SIZE - (pos % BUFFER_SIZE) >= INT_SIZE_IN_BYTES) {
            chunks[pos / BUFFER_SIZE].putInt(value);
            pos += INT_SIZE_IN_BYTES;
            return;
        }
        ensureRemaining(INT_SIZE_IN_BYTES);
        writeByteUnsafe((byte) ((value >>> 24) & 0xFF));
        writeByteUnsafe((byte) ((value >>> 16) & 0xFF));
        writeByteUnsafe((byte) ((value >>> 8) & 0xFF));
        writeByteUnsafe((byte) (value  & 0xFF));
    }

    @Override
    public void writeIntL(int value) {
        ensureRemaining(INT_SIZE_IN_BYTES);
        writeByteUnsafe((byte) (value  & 0xFF));
        writeByteUnsafe((byte) ((value >>> 8) & 0xFF));
        writeByteUnsafe((byte) ((value >>> 16) & 0xFF));
        writeByteUnsafe((byte) ((value >>> 24) & 0xFF));
    }

    @Override
    public void writeLong(long value) {
        if (BUFFER_SIZE - (pos % BUFFER_SIZE) >= LONG_SIZE_IN_BYTES) {
            chunks[pos / BUFFER_SIZE].putLong(value);
            pos += LONG_SIZE_IN_BYTES;
            return;
        }

        ensureRemaining(LONG_SIZE_IN_BYTES);
        writeByteUnsafe((byte) ((value >>> 56) & 0xFF));
        writeByteUnsafe((byte) ((value >>> 48) & 0xFF));
        writeByteUnsafe((byte) ((value >>> 40) & 0xFF));
        writeByteUnsafe((byte) ((value >>> 32) & 0xFF));
        writeByteUnsafe((byte) ((value >>> 24) & 0xFF));
        writeByteUnsafe((byte) ((value >>> 16) & 0xFF));
        writeByteUnsafe((byte) ((value >>> 8) & 0xFF));
        writeByteUnsafe((byte) (value  & 0xFF));
    }

    @Override
    public void write(ByteBuffer src) {
        write(src, src.remaining());
    }

    @Override
    public void write(ByteBuffer src, int count) {
        ensureRemaining(count);
        for (int i = 0; i < count; i++) {
            writeByteUnsafe(src.get());
        }
    }



    @Override
    public int remaining() {
        return totalCapacity - pos;
    }

    private void ensureRemaining(int length) {
        while (remaining() < length) {
            addChunk(allocator.getNextBuffer());
        }
    }

    void addChunk(ByteBuffer chunk) {
        ensureRemainingForNewChunk();
        chunks[chunksPos++] = chunk;
        totalCapacity += BUFFER_SIZE;
    }

    /**
     * TODO: creates litter during warmup, can be replaced with object array pool.
     */
    private void ensureRemainingForNewChunk() {
        if (chunksPos == chunks.length) {
            chunks = Arrays.copyOf(chunks, chunks.length * 2);
        }
    }

    private static class NotImplementedYetException extends RuntimeException {
    }
}
