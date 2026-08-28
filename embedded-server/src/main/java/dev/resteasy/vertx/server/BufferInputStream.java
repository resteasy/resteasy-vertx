/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.server;

import java.io.InputStream;

import io.vertx.core.buffer.Buffer;

/**
 * A zero-copy InputStream that streams directly from a Vert.x Buffer.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class BufferInputStream extends InputStream {

    private final Buffer buffer;
    private final int length;
    private int position;
    private int mark;

    BufferInputStream(final Buffer buffer) {
        this.buffer = buffer;
        this.length = buffer != null ? buffer.length() : 0;
        this.position = 0;
        this.mark = 0;
    }

    @Override
    public int read() {
        if (position >= length) {
            return -1;
        }
        // getByte returns a signed byte; mask with 0xFF to get unsigned int (0-255)
        return buffer.getByte(position++) & 0xFF;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        if (position >= length) {
            return -1;
        }

        int available = length - position;
        int bytesToRead = Math.min(len, available);

        // We avoid buffer.getBytes(start, end) here because that creates a
        // new byte[] array on every read call. The JVM's JIT compiler will
        // heavily optimize this simple loop, making it incredibly fast
        // without allocating anything on the heap.
        for (int i = 0; i < bytesToRead; i++) {
            b[off + i] = buffer.getByte(position++);
        }

        return bytesToRead;
    }

    @Override
    public int available() {
        return length - position;
    }

    @Override
    public long skip(final long n) {
        if (n <= 0) {
            return 0;
        }
        final long available = length - position;
        final long bytesToSkip = Math.min(n, available);
        position += (int) bytesToSkip;
        return bytesToSkip;
    }

    @Override
    public void mark(final int readlimit) {
        mark = position;
    }

    @Override
    public void reset() {
        position = mark;
    }

    @Override
    public boolean markSupported() {
        return true;
    }
}
