package com.github.luben.zstd;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * OutputStream filter that compresses the data using Zstd compression.
 */

public class ZstdOutputStreamNoFinalizer extends FilterOutputStream {

    private static final int dstSize = (int) recommendedCOutSize();

    /* Opaque pointer to Zstd context object */
    private final long stream;
    private final BufferPool bufferPool;
    private final ByteBuffer dstByteBuffer;
    private final byte[] dst;
    private final long dstPos = 0;
    // Source and Destination positions
    private long srcPos;
    private boolean isClosed;
    private boolean closeFrameOnFlush;
    private boolean frameClosed = true;

    /**
     * create a new compressing OutputStream
     *
     * @param outStream the stream to wrap
     * @param level     the compression level
     */
    public ZstdOutputStreamNoFinalizer(OutputStream outStream, int level) throws IOException {
        this(outStream, NoPool.INSTANCE);
        Zstd.setCompressionLevel(stream, level);
    }

    /**
     * create a new compressing OutputStream
     *
     * @param outStream the stream to wrap
     */
    public ZstdOutputStreamNoFinalizer(OutputStream outStream) throws IOException {
        this(outStream, NoPool.INSTANCE);
    }

    /**
     * create a new compressing OutputStream
     *
     * @param outStream  the stream to wrap
     * @param bufferPool the pool to fetch and return buffers
     */
    public ZstdOutputStreamNoFinalizer(OutputStream outStream, BufferPool bufferPool, int level) throws IOException {
        this(outStream, bufferPool);
        Zstd.setCompressionLevel(stream, level);
    }

    /**
     * create a new compressing OutputStream
     *
     * @param outStream  the stream to wrap
     * @param bufferPool the pool to fetch and return buffers
     */
    public ZstdOutputStreamNoFinalizer(OutputStream outStream, BufferPool bufferPool) throws IOException {
        super(outStream);
        // create compression context
        stream = createCStream();
        this.bufferPool = bufferPool;
        dstByteBuffer = bufferPool.get(dstSize);
        if (dstByteBuffer == null) {
            throw new ZstdIOException(Zstd.errMemoryAllocation(), "Cannot get ByteBuffer of size " + dstSize + " from the BufferPool");
        }
        dst = Zstd.extractArray(dstByteBuffer);
    }

    /* JNI methods */
    public static native long recommendedCOutSize();

    private static native long createCStream();

    private static native int freeCStream(long ctx);

    private native int resetCStream(long ctx);

    private native int compressStream(long ctx, byte[] dst, int dst_size, byte[] src, int src_size);

    private native int flushStream(long ctx, byte[] dst, int dst_size);

    private native int endStream(long ctx, byte[] dst, int dst_size);

    /**
     * Enable checksums for the compressed stream.
     * <p>
     * Default: false
     */
    public synchronized ZstdOutputStreamNoFinalizer setChecksum(boolean useChecksums) throws IOException {
        if (!frameClosed) {
            throw new IllegalStateException("Change of parameter on initialized stream");
        }
        int size = Zstd.setCompressionChecksums(stream, useChecksums);
        if (Zstd.isError(size)) {
            throw new ZstdIOException(size);
        }
        return this;
    }

    /**
     * Set the compression level.
     * <p>
     * Default: {@link Zstd#defaultCompressionLevel()}
     */
    public synchronized ZstdOutputStreamNoFinalizer setLevel(int level) throws IOException {
        if (!frameClosed) {
            throw new IllegalStateException("Change of parameter on initialized stream");
        }
        int size = Zstd.setCompressionLevel(stream, level);
        if (Zstd.isError(size)) {
            throw new ZstdIOException(size);
        }
        return this;
    }

    /**
     * Enable Long Distance Matching and set the Window size Log.
     * <p>
     * Values for windowLog outside the range 10-27 will disable and reset LDM
     */
    public synchronized ZstdOutputStreamNoFinalizer setLong(int windowLog) throws IOException {
        if (!frameClosed) {
            throw new IllegalStateException("Change of parameter on initialized stream");
        }
        int size = Zstd.setCompressionLong(stream, windowLog);
        if (Zstd.isError(size)) {
            throw new ZstdIOException(size);
        }
        return this;
    }

    /**
     * Enable use of worker threads for parallel compression.
     * <p>
     * Default: no worker threads.
     */
    public synchronized ZstdOutputStreamNoFinalizer setWorkers(int n) throws IOException {
        if (!frameClosed) {
            throw new IllegalStateException("Change of parameter on initialized stream");
        }
        int size = Zstd.setCompressionWorkers(stream, n);
        if (Zstd.isError(size)) {
            throw new ZstdIOException(size);
        }
        return this;
    }

    /**
     * Enable closing the frame on flush.
     * <p>
     * This will guarantee that it can be ready fully if the process crashes
     * before closing the stream. On the downside it will negatively affect
     * the compression ratio.
     * <p>
     * Default: false.
     */
    public synchronized ZstdOutputStreamNoFinalizer setCloseFrameOnFlush(boolean closeOnFlush) {
        if (!frameClosed) {
            throw new IllegalStateException("Change of parameter on initialized stream");
        }
        closeFrameOnFlush = closeOnFlush;
        return this;
    }

    public synchronized ZstdOutputStreamNoFinalizer setDict(byte[] dict) throws IOException {
        if (!frameClosed) {
            throw new IllegalStateException("Change of parameter on initialized stream");
        }
        int size = Zstd.loadDictCompress(stream, dict, dict.length);
        if (Zstd.isError(size)) {
            throw new ZstdIOException(size);
        }
        return this;
    }

    public synchronized ZstdOutputStreamNoFinalizer setDict(ZstdDictCompress dict) throws IOException {
        if (!frameClosed) {
            throw new IllegalStateException("Change of parameter on initialized stream");
        }
        int size = Zstd.loadFastDictCompress(stream, dict);
        if (Zstd.isError(size)) {
            throw new ZstdIOException(size);
        }
        return this;
    }

    public synchronized void write(byte[] src, int offset, int len) throws IOException {
        if (isClosed) {
            throw new IOException("StreamClosed");
        }
        if (frameClosed) {
            int size = resetCStream(stream);
            if (Zstd.isError(size)) {
                throw new ZstdIOException(size);
            }
            frameClosed = false;
        }
        int srcSize = offset + len;
        srcPos = offset;
        while (srcPos < srcSize) {
            int size = compressStream(stream, dst, dstSize, src, srcSize);
            if (Zstd.isError(size)) {
                throw new ZstdIOException(size);
            }
            if (dstPos > 0) {
                out.write(dst, 0, (int) dstPos);
            }
        }
    }

    public void write(int i) throws IOException {
        byte[] oneByte = new byte[1];
        oneByte[0] = (byte) i;
        write(oneByte, 0, 1);
    }

    /**
     * Flushes the output
     */
    public synchronized void flush() throws IOException {
        if (isClosed) {
            throw new IOException("StreamClosed");
        }
        if (!frameClosed) {
            if (closeFrameOnFlush) {
                // compress the remaining output and close the frame
                int size;
                do {
                    size = endStream(stream, dst, dstSize);
                    if (Zstd.isError(size)) {
                        throw new ZstdIOException(size);
                    }
                    out.write(dst, 0, (int) dstPos);
                } while (size > 0);
                frameClosed = true;
            } else {
                // compress the remaining input
                int size;
                do {
                    size = flushStream(stream, dst, dstSize);
                    if (Zstd.isError(size)) {
                        throw new ZstdIOException(size);
                    }
                    out.write(dst, 0, (int) dstPos);
                } while (size > 0);
            }
            out.flush();
        }
    }


    public synchronized void close() throws IOException {
        close(true);
    }

    public synchronized void closeWithoutClosingParentStream() throws IOException {
        close(false);
    }


    private void close(boolean closeParentStream) throws IOException {
        if (isClosed) {
            return;
        }
        try {
            if (!frameClosed) {
                // compress the remaining input and close the frame
                int size;
                do {
                    size = endStream(stream, dst, dstSize);
                    if (Zstd.isError(size)) {
                        throw new ZstdIOException(size);
                    }
                    out.write(dst, 0, (int) dstPos);
                } while (size > 0);
            }
            if (closeParentStream) {
                out.close();
            }
        } finally {
            // release the resources even if underlying stream throw an exception
            isClosed = true;
            bufferPool.release(dstByteBuffer);
            freeCStream(stream);
        }
    }
}
