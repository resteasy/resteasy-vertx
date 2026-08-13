/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.server.vertx;

import java.io.IOException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.ThreadLocalResteasyProviderFactory;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import dev.resteasy.server.vertx._private.VertxLogger;
import dev.resteasy.vertx.config.ResteasyVertxOptions;

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
    private final SynchronousDispatcher dispatcher;
    private final ResteasyProviderFactory providerFactory;
    private final String contextPath;
    private final long maxRequestSize;

    /**
     * Creates a new routing context handler.
     *
     * @param vertx       the Vert.x instance
     * @param router      the Vert.x Web router
     * @param deployment  the RESTEasy deployment
     * @param contextPath the root path prefix (e.g., "/api")
     */
    VertxRoutingContextHandler(final Vertx vertx, final Router router, final ResteasyDeployment deployment,
            final String contextPath) {
        this.vertx = vertx;
        this.router = router;
        this.dispatcher = (SynchronousDispatcher) deployment.getDispatcher();
        this.providerFactory = deployment.getProviderFactory();
        this.contextPath = contextPath;
        this.maxRequestSize = ResteasyVertxOptions.MAX_REQUEST_SIZE.getValue();
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
        }

        try {
            service(ctx, rc, vertxRequest, vertxResponse);
        } catch (Failure e) {
            if (vertxRequest.getAsyncContext().isSuspended()) {
                vertxRequest.getAsyncContext().getAsyncResponse().resume(e);
            } else {
                vertxResponse.setStatus(e.getErrorCode());
            }
        } catch (Exception ex) {
            if (vertxRequest.getAsyncContext().isSuspended()) {
                vertxRequest.getAsyncContext().getAsyncResponse().resume(ex);
            } else {
                vertxResponse.setStatus(500);
                VertxLogger.LOGGER.failedRequest(ex);
            }
        }

        if (!vertxRequest.getAsyncContext().isSuspended()) {
            if (vertxRequest.wasForwarded()) {
                return;
            }
            try {
                vertxResponse.finish();
            } catch (IOException e) {
                VertxLogger.LOGGER.failedRequest(e);
            }
        }
    }

    private void service(final Context context, final RoutingContext rc,
            final HttpRequest vertxReq, final HttpResponse vertxResp) throws IOException {
        final ResteasyProviderFactory defaultInstance = ResteasyProviderFactory.getInstance();
        if (defaultInstance instanceof ThreadLocalResteasyProviderFactory) {
            ThreadLocalResteasyProviderFactory.push(providerFactory);
        }

        try {
            final SecurityContext securityContext = createSecurityContext(rc);

            ResteasyContext.pushContext(SecurityContext.class, securityContext);
            ResteasyContext.pushContext(Context.class, context);
            ResteasyContext.pushContext(HttpServerRequest.class, rc.request());
            ResteasyContext.pushContext(HttpServerResponse.class, rc.response());
            ResteasyContext.pushContext(Vertx.class, context.owner());
            ResteasyContext.pushContext(RoutingContext.class, rc);
            ResteasyContext.pushContext(Router.class, router);

            dispatcher.invoke(vertxReq, vertxResp);

        } finally {
            try {
                ResteasyContext.clearContextData();
            } finally {
                if (defaultInstance instanceof ThreadLocalResteasyProviderFactory) {
                    ThreadLocalResteasyProviderFactory.pop();
                }
            }
        }
    }

    private SecurityContext createSecurityContext(final RoutingContext rc) {
        final String username = rc.user() != null ? rc.user().subject() : null;
        return new VertxSecurityContext(username, rc.request().isSSL());
    }
}
