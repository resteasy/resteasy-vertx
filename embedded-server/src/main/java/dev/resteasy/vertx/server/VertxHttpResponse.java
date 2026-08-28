/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.ext.RuntimeDelegate;

import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;

import dev.resteasy.vertx.server._private.VertxLogger;

/**
 * Vert.x adapter that bridges {@link HttpServerResponse} to RESTEasy's {@link HttpResponse} SPI.
 * <p>
 * This adapter handles:
 * </p>
 * <ul>
 * <li>Status code and headers conversion to Vert.x format</li>
 * <li>Response body output stream (via {@link ChunkOutputStream})</li>
 * <li>Chunked encoding and response finalization</li>
 * <li>Exception tracking from Vert.x async handlers</li>
 * </ul>
 * <p>
 * The response is considered committed after headers are sent (either via chunked streaming
 * or when {@link #finish()} is called).
 * </p>
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Norman Maurer
 */
public class VertxHttpResponse implements HttpResponse {

    /**
     * Default chunk size for response streaming (1KB).
     */
    private static final int DEFAULT_CHUNK_SIZE = 1024;

    private int status = 200;
    private OutputStream os;
    private MultivaluedMap<String, Object> outputHeaders;
    final HttpServerResponse response;
    private boolean committed;
    private ResteasyProviderFactory providerFactory;
    private volatile Throwable vertxException;
    private boolean ended;

    /**
     * Creates a new response adapter.
     *
     * @param response        the Vert.x HTTP server response
     * @param providerFactory the RESTEasy provider factory for header serialization
     */
    public VertxHttpResponse(final HttpServerResponse response, final ResteasyProviderFactory providerFactory) {
        this(response, providerFactory, null);
    }

    /**
     * Creates a new response adapter with optional HTTP method.
     *
     * @param response        the Vert.x HTTP server response
     * @param providerFactory the RESTEasy provider factory for header serialization
     * @param method          the HTTP method (if HEAD, no output stream is created)
     */
    public VertxHttpResponse(final HttpServerResponse response, final ResteasyProviderFactory providerFactory,
            final HttpMethod method) {
        outputHeaders = new MultivaluedMapImpl<String, Object>();
        os = (method == null || !method.equals(HttpMethod.HEAD)) ? new ChunkOutputStream(this, DEFAULT_CHUNK_SIZE) : null;
        this.response = response;
        this.providerFactory = providerFactory;
        response.exceptionHandler(t -> vertxException = t);
        response.closeHandler(v -> vertxException = new IOException("Connection closed"));
    }

    @Override
    public void setOutputStream(OutputStream os) {
        this.os = os;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public MultivaluedMap<String, Object> getOutputHeaders() {
        return outputHeaders;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return os;
    }

    @Override
    public void addNewCookie(NewCookie cookie) {
        outputHeaders.add(jakarta.ws.rs.core.HttpHeaders.SET_COOKIE, cookie);
    }

    /**
     * Checks if the underlying Vert.x response encountered an exception.
     * <p>
     * Vert.x exceptions (connection closed, write errors) are captured via
     * exception/close handlers and rethrown here as {@link IOException}.
     * </p>
     *
     * @throws IOException if the Vert.x response failed
     */
    void checkException() throws IOException {
        final Throwable vertxException = this.vertxException;
        if (vertxException instanceof IOException)
            throw (IOException) vertxException;
        if (vertxException != null)
            throw new IOException(vertxException);
    }

    @Override
    public void sendError(int status) throws IOException {
        checkException();
        sendError(status, null);
    }

    @Override
    public void sendError(int status, String message) throws IOException {
        checkException();
        if (committed) {
            throw new IllegalStateException();
        }
        response.setStatusCode(status);
        if (message != null) {
            response.end(message);
        } else {
            response.end();
        }
        committed = true;
        ended = true;
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public void reset() {
        if (committed) {
            throw VertxLogger.LOGGER.alreadyCommitted();
        }
        outputHeaders.clear();
    }

    /**
     * Transforms RESTEasy output headers to Vert.x response headers.
     * <p>
     * Uses RESTEasy {@link RuntimeDelegate.HeaderDelegate} for proper header serialization.
     * </p>
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void transformHeaders() {
        for (Map.Entry<String, List<Object>> entry : outputHeaders.entrySet()) {
            String key = entry.getKey();
            for (Object value : entry.getValue()) {
                RuntimeDelegate.HeaderDelegate delegate = providerFactory.getHeaderDelegate(value.getClass());
                if (delegate != null) {
                    response.headers().add(key, delegate.toString(value));
                } else {
                    response.headers().add(key, value.toString());
                }
            }
        }
    }

    /**
     * Prepares the response output stream.
     * <p>
     * Sets the status code, enables chunked mode if applicable, and writes all headers. Called when the response
     * output stream is first written to.
     * </p>
     */
    public void prepareOutputStream() {
        committed = true;
        response.setStatusCode(getStatus());
        transformHeaders();
        // Only use chunked encoding if Content-Length is not set
        if (!outputHeaders.containsKey(jakarta.ws.rs.core.HttpHeaders.CONTENT_LENGTH)) {
            response.setChunked(true);
        }

    }

    /**
     * Prepares an empty response (no body).
     * <p>
     * Sets the status code, writes headers, and ensures Content-Length is removed
     * while keeping the connection alive.
     * </p>
     */
    private void prepareEmptyResponse() {
        committed = true;
        response.setStatusCode(getStatus());
        transformHeaders();
        response.headersEndHandler(h -> {
            response.headers().remove(HttpHeaders.CONTENT_LENGTH);
            response.headers().set(HttpHeaders.CONNECTION, HttpHeaders.KEEP_ALIVE);
        });
    }

    /**
     * Finishes the response by flushing output and ending the Vert.x response.
     * <p>
     * This must be called to complete the HTTP response. If not already committed,
     * headers will be written. The response cannot be modified after this call.
     * </p>
     *
     * @throws IOException if the response has already failed or cannot be finished
     */
    public void finish() throws IOException {
        if (ended)
            return;
        checkException();
        if (os != null) {
            os.flush();
            if (!isCommitted()) {
                prepareOutputStream();
            }
        } else {
            prepareEmptyResponse();
        }
        ended = true;
        response.end();
    }

    @Override
    public void flushBuffer() throws IOException {
        checkException();
        if (os != null) {
            os.flush();
        }
    }
}
