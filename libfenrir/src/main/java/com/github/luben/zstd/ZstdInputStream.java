package com.github.luben.zstd;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * InputStream filter that decompresses the data provided
 * by the underlying InputStream using Zstd compression.
 * <p>
 * It does not support mark/reset methods
 */

public class ZstdInputStream extends FilterInputStream {

    private final ZstdInputStreamNoFinalizer inner;

    /**
     * create a new decompressing InputStream
     *
     * @param inStream the stream to wrap
     */
    public ZstdInputStream(InputStream inStream) throws IOException {
        super(inStream);
        inner = new ZstdInputStreamNoFinalizer(inStream);
    }

    /**
     * create a new decompressing InputStream
     *
     * @param inStream   the stream to wrap
     * @param bufferPool the pool to fetch and return buffers
     */
    public ZstdInputStream(InputStream inStream, BufferPool bufferPool) throws IOException {
        super(inStream);
        inner = new ZstdInputStreamNoFinalizer(inStream, bufferPool);
    }

    public static long recommendedDInSize() {
        return ZstdInputStreamNoFinalizer.recommendedDInSize();
    }

    public static long recommendedDOutSize() {
        return ZstdInputStreamNoFinalizer.recommendedDOutSize();
    }

    /**
     * Enable or disable class finalizers
     *
     * @param finalize default `true` - finalizers are enabled
     * @deprecated If you don't rely on finalizers, use `ZstdInputStreamNoFinalizer` instead, instances of
     * `ZstdInputStream` will always try to close/release in the finalizer.
     */
    @Deprecated
    public void setFinalize(boolean finalize) {
    }

    @Override
    protected void finalize() throws Throwable {
        close();
    }

    public boolean getContinuous() {
        return inner.getContinuous();
    }

    /**
     * Don't break on unfinished frames
     * <p>
     * Use case: decompressing files that are not yet finished writing and compressing
     */
    public ZstdInputStream setContinuous(boolean b) {
        inner.setContinuous(b);
        return this;
    }

    public ZstdInputStream setDict(byte[] dict) throws IOException {
        inner.setDict(dict);
        return this;
    }

    public ZstdInputStream setDict(ZstdDictDecompress dict) throws IOException {
        inner.setDict(dict);
        return this;
    }

    public ZstdInputStream setLongMax(int windowLogMax) throws IOException {
        inner.setLongMax(windowLogMax);
        return this;
    }

    public int read(byte[] dst, int offset, int len) throws IOException {
        return inner.read(dst, offset, len);
    }

    public int read() throws IOException {
        return inner.read();
    }

    public int available() throws IOException {
        return inner.available();
    }


    public long skip(long numBytes) throws IOException {
        return inner.skip(numBytes);
    }

    public boolean markSupported() {
        return inner.markSupported();
    }


    public void close() throws IOException {
        inner.close();
    }
}
