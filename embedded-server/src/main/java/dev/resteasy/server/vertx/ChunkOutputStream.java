/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.server.vertx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.jboss.resteasy.spi.AsyncOutputStream;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;

import dev.resteasy.server.vertx._private.VertxLogger;

/**
 * Buffered output stream for chunked HTTP responses in Vert.x.
 * <p>
 * This stream implements RESTEasy's {@link AsyncOutputStream} interface and provides
 * both blocking and non-blocking write operations. Data is buffered until the chunk
 * size is reached, then written to the Vert.x {@link io.vertx.core.http.HttpServerResponse}.
 * </p>
 * <p>
 * The response is automatically set to chunked transfer encoding on the first flush.
 * </p>
 * <p>
 * <b>Thread-safety:</b> This class is not thread-safe. It should only be used from
 * a single thread (typically the Vert.x event loop for the request).
 * </p>
 *
 * @author tbussier
 */
class ChunkOutputStream extends AsyncOutputStream {
    private Buffer buffer;
    private final VertxHttpResponse response;
    private final int chunkSize;

    /**
     * Creates a new chunk output stream.
     *
     * @param response  the Vert.x HTTP response to write to
     * @param chunkSize the buffer size (must be at least 1)
     *
     * @throws IllegalArgumentException if chunkSize < 1
     */
    ChunkOutputStream(final VertxHttpResponse response, final int chunkSize) {
        this.response = response;
        if (chunkSize < 1) {
            throw VertxLogger.LOGGER.chunkSizeMustBeAtLeastOne();
        }
        this.chunkSize = chunkSize;
        this.buffer = Buffer.buffer(chunkSize);
    }

    @Override
    public void write(int b) throws IOException {
        if (buffer.length() >= chunkSize - 1) {
            flush();
        }
        buffer.appendByte((byte) b);
    }

    @Override
    public void close() throws IOException {
        flush();
        super.close();
    }

    /**
     * Writes a portion of a byte array to the stream.
     * <p>
     * Data is buffered until the chunk size is reached, then automatically flushed.
     * Large writes may result in multiple chunks being written.
     * </p>
     *
     * @param b   the byte array
     * @param off the start offset in the array
     * @param len the number of bytes to write
     *
     * @throws IOException if the response has failed
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        write(b, off, len, null);
    }

    @Override
    public void flush() throws IOException {
        flush(null);
    }

    @Override
    public CompletionStage<Void> asyncFlush() {
        CompletableFuture<Void> ret = new CompletableFuture<>();
        try {
            flush(res -> {
                if (res.succeeded())
                    ret.complete(null);
                else
                    ret.completeExceptionally(res.cause());
            });
        } catch (IOException e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    @Override
    public CompletionStage<Void> asyncWrite(byte[] bytes, int offset, int length) {
        CompletableFuture<Void> ret = new CompletableFuture<>();
        try {
            write(bytes, offset, length, res -> {
                if (res.succeeded())
                    ret.complete(null);
                else
                    ret.completeExceptionally(res.cause());
            });
        } catch (IOException e) {
            ret.completeExceptionally(e);
        }
        return ret;
    }

    /**
     * Internal write implementation that supports both sync and async modes.
     * <p>
     * Algorithm:
     * </p>
     * <ol>
     * <li>While data to write exceeds available buffer space:
     * <ul>
     * <li>Fill buffer to chunk size</li>
     * <li>Flush (tracking future if async)</li>
     * </ul>
     * </li>
     * <li>Append any remaining data to buffer (without flushing)</li>
     * <li>If async, complete when all flushes complete</li>
     * </ol>
     *
     * @param b       the byte array
     * @param off     the start offset
     * @param len     the number of bytes
     * @param handler optional handler for async completion (null for sync)
     *
     * @throws IOException if the response has failed
     */
    private void write(byte[] b, int off, int len, Handler<AsyncResult<CompositeFuture>> handler) throws IOException {
        int dataLengthLeftToWrite = len;
        int dataToWriteOffset = off;
        int spaceLeftInCurrentChunk;
        List<Future<?>> futures;
        if (handler != null) {
            futures = new ArrayList<>();
        } else {
            futures = null;
        }
        while ((spaceLeftInCurrentChunk = chunkSize - buffer.length()) < dataLengthLeftToWrite) {
            buffer.appendBytes(b, dataToWriteOffset, spaceLeftInCurrentChunk);
            dataToWriteOffset = dataToWriteOffset + spaceLeftInCurrentChunk;
            dataLengthLeftToWrite = dataLengthLeftToWrite - spaceLeftInCurrentChunk;
            Promise<Void> promise;
            if (handler != null) {
                promise = Promise.promise();
                futures.add(promise.future());
            } else {
                promise = null;
            }
            flush(promise == null ? null : promise::handle);
        }
        if (dataLengthLeftToWrite > 0) {
            buffer.appendBytes(b, dataToWriteOffset, dataLengthLeftToWrite);
        }
        if (handler != null) {
            Future.all(futures).onComplete(handler);
        }
    }

    /**
     * Internal flush implementation that supports both sync and async modes.
     *
     * @param handler optional handler for async completion (null for sync)
     *
     * @throws IOException if the response has failed
     */
    private void flush(Handler<AsyncResult<Void>> handler) throws IOException {
        int readable = buffer.length();
        if (readable == 0) {
            if (handler != null)
                handler.handle(Future.succeededFuture());
            return;
        }
        if (!response.isCommitted())
            response.prepareOutputStream();
        response.checkException();
        Future<Void> future = response.response.write(buffer);
        if (handler != null) {
            future.onComplete(handler);
        }
        buffer = Buffer.buffer();
        super.flush();
    }

}
