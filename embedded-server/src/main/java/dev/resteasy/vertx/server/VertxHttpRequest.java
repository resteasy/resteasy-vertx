/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.core.AbstractAsynchronousResponse;
import org.jboss.resteasy.core.AbstractExecutionContext;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.core.ResteasyContext.CloseableContext;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.plugins.server.BaseHttpRequest;
import org.jboss.resteasy.specimpl.ResteasyHttpHeaders;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyAsynchronousContext;
import org.jboss.resteasy.spi.ResteasyAsynchronousResponse;
import org.jboss.resteasy.spi.RunnableWithException;

import io.vertx.core.Context;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;

import dev.resteasy.vertx.server._private.VertxLogger;

/**
 * Vert.x adapter that bridges {@link HttpServerRequest} to RESTEasy's {@link HttpRequest} SPI.
 * <p>
 * This adapter handles:
 * </p>
 * <ul>
 * <li>Request headers and URI conversion to RESTEasy format</li>
 * <li>Request body input stream from Vert.x buffers</li>
 * <li>Request attributes storage</li>
 * <li>JAX-RS async context (via nested {@link VertxExecutionContext})</li>
 * <li>Remote address information</li>
 * </ul>
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Norman Maurer
 * @author Kristoffer Sjogren
 */
class VertxHttpRequest extends BaseHttpRequest {
    private final ResteasyHttpHeaders httpHeaders;
    private final RoutingContext routingContext;
    private final Map<String, Object> attributes;
    private final VertxExecutionContext executionContext;
    private final Context context;
    private final HttpServerRequest request;
    private String httpMethod;
    private InputStream inputStream;
    private boolean forwarded;

    /**
     * Creates a new Vert.x HTTP request adapter.
     *
     * @param context        the Vert.x context for this request
     * @param routingContext the Vert.x Web routing context
     * @param uri            the parsed URI information
     * @param dispatcher     the RESTEasy synchronous dispatcher
     * @param response       the associated response adapter
     */
    VertxHttpRequest(final Context context, final RoutingContext routingContext, final ResteasyUriInfo uri,
            final SynchronousDispatcher dispatcher, final VertxHttpResponse response) {
        super(uri);
        this.context = context;
        this.routingContext = routingContext;
        this.request = routingContext.request();
        this.attributes = routingContext.data();
        this.httpHeaders = VertxUtil.extractHttpHeaders(request);
        this.httpMethod = request.method().name();
        this.executionContext = new VertxExecutionContext(this, response, dispatcher);
    }

    @Override
    public MultivaluedMap<String, String> getMutableHeaders() {
        return httpHeaders.getMutableHeaders();
    }

    @Override
    public void setHttpMethod(String method) {
        this.httpMethod = method;
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public ResteasyAsynchronousContext getAsyncContext() {
        return executionContext;
    }

    @Override
    public Object getAttribute(String attribute) {
        return attributes.get(attribute);
    }

    @Override
    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        return httpHeaders;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public void setInputStream(InputStream stream) {
        this.inputStream = stream;
    }

    @Override
    public String getHttpMethod() {
        return httpMethod;
    }

    @Override
    public void forward(String path) {
        forwarded = true;
        routingContext.reroute(path);
    }

    @Override
    public boolean wasForwarded() {
        return forwarded;
    }

    /**
     * Vert.x implementation of RESTEasy's async execution context.
     * <p>
     * This handles JAX-RS {@code @Suspended} async responses and manages the lifecycle
     * of async request processing on the Vert.x event loop.
     * </p>
     * <p>
     * When a JAX-RS resource method is suspended, this context manages resumption
     * either on the event loop or via blocking execution on a worker thread.
     * </p>
     */
    class VertxExecutionContext extends AbstractExecutionContext {
        protected final VertxHttpRequest request;
        protected final VertxHttpResponse response;
        protected volatile boolean done;
        protected volatile boolean cancelled;
        protected volatile boolean wasSuspended;
        protected VertxHttpAsyncResponse asyncResponse;

        VertxExecutionContext(final VertxHttpRequest request, final VertxHttpResponse response,
                final SynchronousDispatcher dispatcher) {
            super(dispatcher, request, response);
            this.request = request;
            this.response = response;
            this.asyncResponse = new VertxHttpAsyncResponse(dispatcher, request, response);
        }

        @Override
        public boolean isSuspended() {
            return wasSuspended;
        }

        @Override
        public ResteasyAsynchronousResponse getAsyncResponse() {
            return asyncResponse;
        }

        @Override
        public ResteasyAsynchronousResponse suspend() throws IllegalStateException {
            return suspend(-1);
        }

        @Override
        public ResteasyAsynchronousResponse suspend(long millis) throws IllegalStateException {
            return suspend(millis, TimeUnit.MILLISECONDS);
        }

        @Override
        public ResteasyAsynchronousResponse suspend(long time, TimeUnit unit) throws IllegalStateException {
            if (wasSuspended) {
                throw VertxLogger.LOGGER.alreadySuspended();
            }
            wasSuspended = true;
            if (time > 0) {
                asyncResponse.setTimeout(time, unit);
            }
            return asyncResponse;
        }

        @Override
        public void complete() {
            if (wasSuspended && asyncResponse != null)
                asyncResponse.complete();
        }

        /**
         * Vert.x implementation of JAX-RS {@link AsyncResponse}.
         * <p>
         * Manages the async response lifecycle including:
         * </p>
         * <ul>
         * <li>Resume with entity or exception</li>
         * <li>Cancellation with retry-after headers</li>
         * <li>Timeout handling via Vert.x timers</li>
         * </ul>
         * <p>
         * <b>Thread-safety:</b> All methods are synchronized on {@code responseLock} to ensure
         * safe concurrent access from JAX-RS async operations and Vert.x event loop callbacks.
         * </p>
         *
         * @author Kristoffer Sjogren
         */
        class VertxHttpAsyncResponse extends AbstractAsynchronousResponse {
            private final Object responseLock = new Object();
            private long timerID = -1;
            private VertxHttpResponse vertxResponse;

            VertxHttpAsyncResponse(final SynchronousDispatcher dispatcher, final VertxHttpRequest request,
                    final VertxHttpResponse response) {
                super(dispatcher, request, response);
                this.vertxResponse = response;
            }

            @Override
            public void initialRequestThreadFinished() {
                // done
            }

            @Override
            public void complete() {
                synchronized (responseLock) {
                    if (done)
                        return;
                    if (cancelled)
                        return;
                    done = true;
                    vertxFlush();
                }
            }

            @Override
            public boolean resume(Object entity) {
                synchronized (responseLock) {
                    if (done)
                        return false;
                    if (cancelled)
                        return false;
                    done = true;
                    return internalResume(entity, t -> vertxFlush());
                }
            }

            @Override
            public boolean resume(Throwable ex) {
                synchronized (responseLock) {
                    if (done)
                        return false;
                    if (cancelled)
                        return false;
                    done = true;
                    return internalResume(ex, t -> vertxFlush());
                }
            }

            @Override
            public boolean cancel() {
                synchronized (responseLock) {
                    if (cancelled) {
                        return true;
                    }
                    if (done) {
                        return false;
                    }
                    done = true;
                    cancelled = true;
                    return internalResume(Response.status(Response.Status.SERVICE_UNAVAILABLE).build(), t -> vertxFlush());
                }
            }

            @Override
            public boolean cancel(int retryAfter) {
                synchronized (responseLock) {
                    if (cancelled)
                        return true;
                    if (done)
                        return false;
                    done = true;
                    cancelled = true;
                    return internalResume(
                            Response.status(Response.Status.SERVICE_UNAVAILABLE).header(HttpHeaders.RETRY_AFTER, retryAfter)
                                    .build(),
                            t -> vertxFlush());
                }
            }

            protected synchronized void vertxFlush() {
                try {
                    vertxResponse.finish();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean cancel(Date retryAfter) {
                synchronized (responseLock) {
                    if (cancelled)
                        return true;
                    if (done)
                        return false;
                    done = true;
                    cancelled = true;
                    return internalResume(
                            Response.status(Response.Status.SERVICE_UNAVAILABLE).header(HttpHeaders.RETRY_AFTER, retryAfter)
                                    .build(),
                            t -> vertxFlush());
                }
            }

            @Override
            public boolean isSuspended() {
                return !done && !cancelled;
            }

            @Override
            public boolean isCancelled() {
                return cancelled;
            }

            @Override
            public boolean isDone() {
                return done;
            }

            @Override
            public boolean setTimeout(long time, TimeUnit unit) {
                synchronized (responseLock) {
                    if (done || cancelled)
                        return false;
                    if (timerID > -1 && !context.owner().cancelTimer(timerID)) {
                        return false;
                    }
                    timerID = context.owner().setTimer(unit.toMillis(time), v -> handleTimeout());
                }
                return true;
            }

            protected void handleTimeout() {
                if (timeoutHandler != null) {
                    timeoutHandler.handleTimeout(this);
                    return;
                }
                if (done)
                    return;
                resume(new ServiceUnavailableException());
            }
        }

        @Override
        public CompletionStage<Void> executeAsyncIo(CompletionStage<Void> f) {
            // check if this CF is already resolved
            CompletableFuture<Void> ret = f.toCompletableFuture();
            // if it's not resolved, we may need to suspend
            if (!ret.isDone() && !isSuspended()) {
                suspend();
            }
            return ret;
        }

        @Override
        public CompletionStage<Void> executeBlockingIo(RunnableWithException f, boolean hasInterceptors) {
            if (!Context.isOnEventLoopThread()) {
                // we're blocking
                try {
                    f.run();
                } catch (Exception e) {
                    CompletableFuture<Void> ret = new CompletableFuture<>();
                    ret.completeExceptionally(e);
                    return ret;
                }
                return CompletableFuture.completedFuture(null);
            } else if (!hasInterceptors) {
                Map<Class<?>, Object> context = ResteasyContext.getContextDataMap();
                // turn any sync request into async
                if (!isSuspended()) {
                    suspend();
                }
                CompletableFuture<Void> ret = new CompletableFuture<>();
                this.request.context.executeBlocking(() -> {
                    try (CloseableContext newContext = ResteasyContext.addCloseableContextDataLevel(context)) {
                        f.run();
                        return null;
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).onComplete(res -> {
                    if (res.succeeded())
                        ret.complete(null);
                    else
                        ret.completeExceptionally(res.cause());
                });
                return ret;
            } else {
                CompletableFuture<Void> ret = new CompletableFuture<>();
                ret.completeExceptionally(
                        new RuntimeException("Cannot use blocking IO with interceptors when we're on the IO thread"));
                return ret;
            }
        }
    }

    @Override
    public String getRemoteHost() {
        final SocketAddress remoteAddress = request.remoteAddress();
        return remoteAddress != null ? remoteAddress.host() : null;
    }

    @Override
    public String getRemoteAddress() {
        final SocketAddress remoteAddress = request.remoteAddress();
        return remoteAddress != null ? remoteAddress.host() : null;
    }
}
