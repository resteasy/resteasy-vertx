/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.client.jaxrs.engines.vertx;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;

/**
 * Adapts a Vert.x {@link ReadStream} of {@link Buffer} to a blocking {@link InputStream}.
 * <p>
 * This adapter provides backpressure by pausing the stream when the pending buffer exceeds
 * {@code maxPendingSize} and resuming when it drains below the resume threshold (50% of max).
 * This prevents excessive memory consumption while maintaining efficient throughput.
 * </p>
 * <p>
 * Thread-safe for concurrent read operations, though typical usage is from a single reader thread.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class InputStreamAdapter extends InputStream {

    private final Lock lock = new ReentrantLock();
    private final Condition dataAvailable = lock.newCondition();

    private final ReadStream<Buffer> stream;
    private final long maxPendingSize;
    private final long resumeThreshold;
    private Buffer pending = Buffer.buffer();
    private int readPosition;
    private boolean paused;
    private boolean closed;
    private boolean ended;
    private Throwable failure;

    /**
     * Creates an adapter with a default maximum pending buffer size of 64KB.
     *
     * @param stream the Vert.x read stream to adapt
     */
    public InputStreamAdapter(final ReadStream<Buffer> stream) {
        // Default to 64KB to avoid throttling
        this(stream, 65536);
    }

    /**
     * Creates an adapter with the specified maximum pending buffer size.
     * <p>
     * The stream will be paused when the pending buffer exceeds {@code maxPendingSize}
     * and resumed when it drains below 50% of the maximum to prevent pause/resume thrashing.
     * </p>
     *
     * @param stream         the Vert.x read stream to adapt
     * @param maxPendingSize maximum bytes to buffer before pausing the stream
     */
    public InputStreamAdapter(final ReadStream<Buffer> stream, final long maxPendingSize) {
        this.stream = stream;
        this.maxPendingSize = maxPendingSize;
        // Resume when buffer drains below 50% to prevent constant pause/resume thrashing
        this.resumeThreshold = maxPendingSize / 2;

        stream.handler(this::onChunk);
        stream.endHandler(this::onEnd);
        stream.exceptionHandler(this::onError);
    }

    @Override
    public int available() {
        lock.lock();
        try {
            return pendingBytes();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int read() throws IOException {
        final byte[] buf = new byte[1];
        int val = read(buf, 0, 1);
        if (val == -1) {
            return -1;
        }
        return buf[0] & 0xFF;
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        if (len == 0) {
            return 0;
        }

        lock.lock();
        try {
            while (true) {
                final int available = pendingBytes();
                if (available > 0) {
                    final int amount = Math.min(available, len);
                    pending.getBytes(readPosition, readPosition + amount, b, off);
                    readPosition += amount;

                    // Compact when more than half consumed
                    if (readPosition > pending.length() / 2) {
                        pending = pending.getBuffer(readPosition, pending.length());
                        readPosition = 0;
                    }

                    // Resume stream if we've drained below the threshold
                    if (paused && pendingBytes() <= resumeThreshold) {
                        paused = false;
                        stream.resume();
                    }

                    return amount;
                } else {
                    if (ended) {
                        if (failure != null) {
                            throw new IOException("Vert.x stream error", failure);
                        } else {
                            // EOF
                            return -1;
                        }
                    }
                    try {
                        dataAvailable.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedIOException("Interrupted while waiting for stream data");
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            if (closed) {
                return;
            }
            closed = true;
            ended = true;
            pending = Buffer.buffer();
            readPosition = 0;
            dataAvailable.signalAll();
        } finally {
            lock.unlock();
        }
        stream.pause();
        stream.handler(null);
        stream.exceptionHandler(null);
        stream.endHandler(null);
    }

    private int pendingBytes() {
        return pending.length() - readPosition;
    }

    private void onChunk(final Buffer chunk) {
        lock.lock();
        try {
            pending.appendBuffer(chunk);
            if (pending.length() > maxPendingSize && !paused) {
                paused = true;
                stream.pause();
            }
            dataAvailable.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void onEnd(final Void v) {
        lock.lock();
        try {
            ended = true;
            dataAvailable.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void onError(final Throwable cause) {
        lock.lock();
        try {
            failure = cause;
            ended = true;
            dataAvailable.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
