/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.server.vertx;

import java.io.IOException;
import java.security.Principal;
import java.util.Base64;
import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.ThreadLocalResteasyProviderFactory;
import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.HttpResponseCodes;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.HttpHeaderNames;

import io.netty.buffer.ByteBufInputStream;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.internal.buffer.BufferInternal;

import dev.resteasy.server.vertx._private.VertxLogger;
import dev.resteasy.vertx.config.ResteasyVertxOptions;

/**
 * Vert.x HTTP request handler that integrates with RESTEasy.
 * <p>
 * This handler processes incoming HTTP requests by:
 * </p>
 * <ul>
 * <li>Buffering the request body</li>
 * <li>Setting up RESTEasy context (security, Vert.x context, provider factory)</li>
 * <li>Optionally authenticating via {@link SecurityDomain} (basic auth)</li>
 * <li>Dispatching to RESTEasy for resource method invocation</li>
 * <li>Finalizing the response if not async</li>
 * </ul>
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class VertxRequestHandler implements Handler<HttpServerRequest> {

    private final long maxRequestSize;
    private final Vertx vertx;
    private final SynchronousDispatcher dispatcher;
    private final ResteasyProviderFactory providerFactory;
    private final String contextPath;
    private final SecurityDomain securityDomain;

    /**
     * Creates a new request handler.
     *
     * @param vertx          the Vert.x instance
     * @param deployment     the RESTEasy deployment
     * @param contextPath    the root path prefix (e.g., "/api")
     * @param securityDomain optional security domain for basic authentication
     */
    public VertxRequestHandler(final Vertx vertx, final ResteasyDeployment deployment,
            final String contextPath, final SecurityDomain securityDomain) {
        this.vertx = vertx;
        this.dispatcher = (SynchronousDispatcher) deployment.getDispatcher();
        this.providerFactory = deployment.getProviderFactory();
        this.contextPath = contextPath;
        this.securityDomain = securityDomain;
        this.maxRequestSize = ResteasyVertxOptions.MAX_REQUEST_SIZE.getValue();
    }

    @Override
    public void handle(HttpServerRequest request) {
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

        request.bodyHandler(buff -> {
            if (maxRequestSize > 0 && buff.length() > maxRequestSize) {
                request.response().setStatusCode(Response.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode()).end();
                return;
            }
            Context ctx = vertx.getOrCreateContext();
            ResteasyUriInfo uriInfo = VertxUtil.extractUriInfo(request, contextPath);
            HttpServerResponse response = request.response();
            VertxHttpResponse vertxResponse = new VertxHttpResponse(response, providerFactory, request.method());
            VertxHttpRequest vertxRequest = new VertxHttpRequest(ctx, request, uriInfo, dispatcher, vertxResponse, false);

            // Set request body if present
            if (buff.length() > 0) {
                BufferInternal bufferInternal = (BufferInternal) buff;
                ByteBufInputStream in = new ByteBufInputStream(bufferInternal.getByteBuf());
                vertxRequest.setInputStream(in);
            }

            try {
                service(ctx, request, response, vertxRequest, vertxResponse);
            } catch (Failure e) {
                vertxResponse.setStatus(e.getErrorCode());
            } catch (Exception ex) {
                vertxResponse.setStatus(500);
                VertxLogger.LOGGER.failedRequest(ex);
            }

            // Finish response if not async
            if (!vertxRequest.getAsyncContext().isSuspended()) {
                try {
                    vertxResponse.finish();
                } catch (IOException e) {
                    VertxLogger.LOGGER.failedRequest(e);
                }
            }
        });
    }

    /**
     * Services the request by setting up RESTEasy context and dispatching.
     * <p>
     * This method:
     * </p>
     * <ul>
     * <li>Pushes the provider factory to thread-local if needed</li>
     * <li>Authenticates the request if a security domain is configured</li>
     * <li>Pushes RESTEasy context (SecurityContext, Vert.x Context, etc.)</li>
     * <li>Invokes the RESTEasy dispatcher</li>
     * <li>Cleans up context in finally block</li>
     * </ul>
     */
    private void service(Context context, HttpServerRequest req, HttpServerResponse resp,
            HttpRequest vertxReq, HttpResponse vertxResp) throws IOException {
        // Push provider factory to thread-local if needed
        ResteasyProviderFactory defaultInstance = ResteasyProviderFactory.getInstance();
        if (defaultInstance instanceof ThreadLocalResteasyProviderFactory) {
            ThreadLocalResteasyProviderFactory.push(providerFactory);
        }

        try {
            // Authenticate if security domain is configured
            SecurityContext securityContext;
            if (securityDomain != null) {
                securityContext = authenticate(vertxReq, vertxResp);
                if (securityContext == null) {
                    // Authentication failed, 401 already sent
                    return;
                }
            } else {
                securityContext = new VertxSecurityContext();
            }

            // Push RESTEasy context
            ResteasyContext.pushContext(SecurityContext.class, securityContext);
            ResteasyContext.pushContext(Context.class, context);
            ResteasyContext.pushContext(HttpServerRequest.class, req);
            ResteasyContext.pushContext(HttpServerResponse.class, resp);
            ResteasyContext.pushContext(Vertx.class, context.owner());

            // Dispatch to RESTEasy
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

    /**
     * Authenticates the request using basic authentication.
     *
     * @param request  the RESTEasy HTTP request
     * @param response the RESTEasy HTTP response
     * @return the security context if authentication succeeds, null if it fails (401 sent)
     * @throws IOException if sending the error response fails
     */
    private SecurityContext authenticate(HttpRequest request, HttpResponse response) throws IOException {
        List<String> headers = request.getHttpHeaders().getRequestHeader(HttpHeaderNames.AUTHORIZATION);
        if (!headers.isEmpty()) {
            String auth = headers.get(0);
            if (auth.length() > 5) {
                String type = auth.substring(0, 5);
                type = type.toLowerCase();
                if ("basic".equals(type)) {
                    String cookie = auth.substring(6);
                    cookie = new String(Base64.getDecoder().decode(cookie.getBytes()));
                    String[] split = cookie.split(":", 2);
                    if (split.length < 2) {
                        response.sendError(HttpResponseCodes.SC_UNAUTHORIZED);
                        return null;
                    }
                    Principal user = null;
                    try {
                        user = securityDomain.authenticate(split[0], split[1]);
                        return new VertxSecurityContext(user, securityDomain, "BASIC", true);
                    } catch (SecurityException e) {
                        response.sendError(HttpResponseCodes.SC_UNAUTHORIZED);
                        return null;
                    }
                } else {
                    response.sendError(HttpResponseCodes.SC_UNAUTHORIZED);
                    return null;
                }
            }
        }
        response.sendError(HttpResponseCodes.SC_UNAUTHORIZED);
        return null;
    }
}
