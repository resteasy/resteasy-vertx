/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.client.jaxrs.engines.vertx;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.vertx.core.Context;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;

/**
 * An {@link OutputStream} that writes data to a Vert.x {@link HttpClientRequest} in chunks.
 * <p>
 * This stream handles backpressure by blocking the writing thread when the Vert.x write queue
 * is full, waiting for the drain handler to signal that more data can be written. This ensures
 * memory is not exhausted when writing large request bodies.
 * </p>
 * <p>
 * All interactions with the Vert.x request are executed on the provided event loop {@link Context}
 * to ensure thread safety with Vert.x's threading model. The calling thread blocks via a
 * {@link CountDownLatch} when necessary to maintain backpressure.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class VertxChunkedOutputStream extends OutputStream {
    private static final long AWAIT_TIMEOUT_SECONDS = 60;

    private final HttpClientRequest request;
    private final Context context;
    private final AtomicReference<Throwable> error = new AtomicReference<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Creates a chunked output stream that writes to the given Vert.x HTTP client request.
     *
     * @param request the Vert.x HTTP client request to write to
     * @param context the Vert.x context on which to execute write operations
     */
    VertxChunkedOutputStream(final HttpClientRequest request, final Context context) {
        this.request = request;
        this.context = context;
    }

    @Override
    public void write(final int b) throws IOException {
        write(new byte[] { (byte) b }, 0, 1);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        checkError();
        final Buffer chunk = Buffer.buffer(len).appendBytes(b, off, len);
        final CountDownLatch latch = new CountDownLatch(1);

        // Vert.x requires interacting with the request on its Event Loop context
        context.runOnContext(v -> {
            request.write(chunk).onFailure(t -> {
                error.compareAndSet(null, t);
                latch.countDown();
            });

            // Backpressure: Only block the JAX-RS thread if the Vert.x write queue is full
            if (request.writeQueueFull()) {
                request.drainHandler(drain -> {
                    request.drainHandler(null); // clear handler
                    latch.countDown();
                });
            } else {
                latch.countDown(); // Do not block, continue writing immediately
            }
        });

        try {
            if (!latch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                throw new IOException("Timed out waiting for Vert.x write to complete");
            }
            checkError();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while writing to Vert.x stream");
        }
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            checkError();
            final CountDownLatch latch = new CountDownLatch(1);

            context.runOnContext(v -> {
                request.end().onComplete(ar -> {
                    if (ar.failed()) {
                        error.compareAndSet(null, ar.cause());
                    }
                    latch.countDown();
                });
            });

            try {
                if (!latch.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                    throw new IOException("Timed out waiting for Vert.x stream to close");
                }
                checkError();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("Interrupted while closing Vert.x stream");
            }
        }
    }

    private void checkError() throws IOException {
        Throwable t = error.get();
        if (t != null) {
            throw new IOException("Vert.x write failed", t);
        }
    }
}
