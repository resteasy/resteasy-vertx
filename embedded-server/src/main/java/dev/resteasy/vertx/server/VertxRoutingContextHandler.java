/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.server;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.core.ThreadLocalResteasyProviderFactory;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.jboss.resteasy.spi.ResourceInvoker;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import dev.resteasy.vertx.config.ResteasyVertxOptions;
import dev.resteasy.vertx.server.annotations.Blocking;
import dev.resteasy.vertx.server.annotations.NonBlocking;

/**
 * Vert.x Web request handler that integrates with RESTEasy via {@link RoutingContext}.
 * <p>
 * This handler processes incoming HTTP requests by:
 * </p>
 * <ul>
 * <li>Buffering the request body via {@link HttpServerRequest#body()}</li>
 * <li>Enforcing request size limits via {@link ResteasyVertxOptions#MAX_REQUEST_SIZE}</li>
 * <li>Setting up RESTEasy context (security, Vert.x context, provider factory)</li>
 * <li>Dispatching to RESTEasy for resource method invocation</li>
 * <li>Finalizing the response if not async</li>
 * </ul>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class VertxRoutingContextHandler implements Handler<RoutingContext> {

    private static final String BODY_BUFFER_KEY = "dev.resteasy.vertx.server.body";

    private final Vertx vertx;
    private final Router router;
    private final VertxSynchronousDispatcher dispatcher;
    private final ResteasyProviderFactory providerFactory;
    private final String contextPath;
    private final long maxRequestSize;
    private final boolean defaultBlocking;

    /**
     * Creates a new routing context handler.
     *
     * @param vertx       the Vert.x instance
     * @param router      the Vert.x Web router
     * @param deployment  the RESTEasy deployment
     * @param contextPath the root path prefix (e.g., "/api")
     */
    VertxRoutingContextHandler(final Vertx vertx, final Router router, final VertxResteasyDeployment deployment,
            final String contextPath) {
        this.vertx = vertx;
        this.router = router;
        this.dispatcher = (VertxSynchronousDispatcher) deployment.getDispatcher();
        this.providerFactory = deployment.getProviderFactory();
        this.contextPath = contextPath;
        this.maxRequestSize = ResteasyVertxOptions.MAX_REQUEST_SIZE.getValue();
        this.defaultBlocking = ResteasyVertxOptions.DEFAULT_BLOCKING.getValue();
    }

    @Override
    public void handle(final RoutingContext rc) {
        final HttpServerRequest request = rc.request();

        // Check for cached body from a previous pass (e.g., rerouted request)
        final Buffer cachedBody = (Buffer) rc.data().get(BODY_BUFFER_KEY);
        if (cachedBody != null) {
            dispatch(rc, cachedBody);
            return;
        }

        // Fast reject based on Content-Length header
        if (maxRequestSize > 0) {
            final String contentLength = request.getHeader("Content-Length");
            if (contentLength != null) {
                try {
                    if (Long.parseLong(contentLength) > maxRequestSize) {
                        request.response().setStatusCode(Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()).end();
                        return;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        request.body().onSuccess(body -> {
            if (maxRequestSize > 0 && body != null && body.length() > maxRequestSize) {
                request.response().setStatusCode(Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()).end();
                return;
            }
            // Cache the body for potential reroutes
            if (body != null) {
                rc.data().put(BODY_BUFFER_KEY, body);
            }
            dispatch(rc, body);
        }).onFailure(rc::fail);
    }

    private void dispatch(final RoutingContext rc, final Buffer body) {
        final HttpServerRequest request = rc.request();
        final Context ctx = vertx.getOrCreateContext();
        final ResteasyUriInfo uriInfo = VertxUtil.extractUriInfo(request, contextPath);
        final HttpServerResponse response = request.response();
        final VertxHttpResponse vertxResponse = new VertxHttpResponse(response, providerFactory, request.method());
        final VertxHttpRequest vertxRequest = new VertxHttpRequest(ctx, rc, uriInfo, dispatcher, vertxResponse);

        if (body != null && body.length() > 0) {
            vertxRequest.setInputStream(new BufferInputStream(body));
        } else {
            vertxRequest.setInputStream(InputStream.nullInputStream());
        }

        final ResteasyProviderFactory defaultInstance = ResteasyProviderFactory.getInstance();
        final boolean pushedProviderFactory = defaultInstance instanceof ThreadLocalResteasyProviderFactory;
        if (pushedProviderFactory) {
            ThreadLocalResteasyProviderFactory.push(providerFactory);
        }
        try {
            final SecurityContext securityContext = createSecurityContext(rc);
            ResteasyContext.pushContext(SecurityContext.class, securityContext);
            ResteasyContext.pushContext(Context.class, ctx);
            ResteasyContext.pushContext(HttpServerRequest.class, rc.request());
            ResteasyContext.pushContext(HttpServerResponse.class, rc.response());
            ResteasyContext.pushContext(Vertx.class, ctx.owner());
            ResteasyContext.pushContext(RoutingContext.class, rc);
            ResteasyContext.pushContext(Router.class, router);
            dispatcher.pushContextObjects(vertxRequest, vertxResponse);

            // Pre-match filters (sync or async) run here, exactly once. Once they let the request through,
            // the continuation resolves the invoker and decides whether to dispatch to a worker thread.
            dispatcher.preprocess(vertxRequest, vertxResponse, () -> invokeResource(vertxRequest, vertxResponse));
        } catch (Exception e) {
            // Something escaped preprocessing/invoker resolution itself (not a resource/filter exception -
            // those are already mapped and written by the dispatcher). Map and finish here as a last resort.
            writeException(vertxRequest, vertxResponse, e);
        } finally {
            ResteasyContext.clearContextData();
            if (pushedProviderFactory) {
                ThreadLocalResteasyProviderFactory.pop();
            }
        }
    }

    private void invokeResource(final VertxHttpRequest vertxRequest, final VertxHttpResponse vertxResponse) {
        final ResourceInvoker invoker;
        try {
            invoker = dispatcher.getInvoker(vertxRequest);
        } catch (Exception e) {
            writeException(vertxRequest, vertxResponse, e);
            return;
        }

        if (isBlocking(invoker)) {
            // Capture RESTEasy context for the worker thread
            final Map<Class<?>, Object> contextMap = ResteasyContext.getContextDataMap();

            vertx.executeBlocking(() -> {
                final ResteasyProviderFactory defaultInstance = ResteasyProviderFactory.getInstance();
                final boolean pushedProviderFactory = defaultInstance instanceof ThreadLocalResteasyProviderFactory;
                if (pushedProviderFactory) {
                    ThreadLocalResteasyProviderFactory.push(providerFactory);
                }
                try (ResteasyContext.CloseableContext ignored = ResteasyContext.addCloseableContextDataLevel(contextMap)) {
                    dispatcher.invoke(vertxRequest, vertxResponse, invoker);
                    return null;
                } finally {
                    if (pushedProviderFactory) {
                        ThreadLocalResteasyProviderFactory.pop();
                    }
                }
            }, false).onComplete(ar -> {
                if (ar.failed()) {
                    // invoke() already maps and writes (and finishes) almost every exception itself; this only
                    // catches something that truly escaped it (e.g. an UnhandledException).
                    writeException(vertxRequest, vertxResponse, ar.cause());
                }
            });
        } else {
            try {
                dispatcher.invoke(vertxRequest, vertxResponse, invoker);
            } catch (Exception e) {
                writeException(vertxRequest, vertxResponse, e);
            }
        }
    }

    private void writeException(final VertxHttpRequest vertxRequest, final VertxHttpResponse vertxResponse,
            final Throwable cause) {
        if (vertxRequest.getAsyncContext().isSuspended()) {
            vertxRequest.getAsyncContext().getAsyncResponse().resume(cause);
            return;
        }
        dispatcher.writeException(vertxRequest, vertxResponse, cause, t -> {
        });
    }

    private SecurityContext createSecurityContext(final RoutingContext rc) {
        final String username = rc.user() != null ? rc.user().subject() : null;
        return new VertxSecurityContext(username, rc.request().isSSL());
    }

    private boolean isBlocking(final ResourceInvoker invoker) {
        final Method method = invoker.getMethod();
        if (method.isAnnotationPresent(Blocking.class)) {
            return true;
        } else if (method.isAnnotationPresent(NonBlocking.class)) {
            return false;
        } else if (method.getDeclaringClass().isAnnotationPresent(Blocking.class)) {
            return true;
        } else if (method.getDeclaringClass().isAnnotationPresent(NonBlocking.class)) {
            return false;
        } else if (!defaultBlocking) {
            return false;
        }
        return !CompletionStage.class.isAssignableFrom(method.getReturnType()) && !hasAsyncResponseParameter(method);
    }

    private boolean hasAsyncResponseParameter(final Method method) {
        return Stream.of(method.getParameters())
                .anyMatch(p -> p.isAnnotationPresent(Suspended.class));
    }
}
