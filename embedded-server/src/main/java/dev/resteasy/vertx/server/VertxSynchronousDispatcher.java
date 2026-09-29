/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.server;

import java.io.IOException;
import java.util.function.Consumer;

import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import dev.resteasy.vertx.server._private.VertxLogger;

/**
 * A {@link SynchronousDispatcher} for the Vert.x integration.
 * <p>
 * Widens access to the continuation-based {@link #preprocess(HttpRequest, HttpResponse, Runnable)} so
 * {@link VertxRoutingContextHandler} can resolve the {@link org.jboss.resteasy.spi.ResourceInvoker} once,
 * after pre-match filters have run (including asynchronous ones), before deciding whether to dispatch the
 * resource method invocation to a worker thread.
 * </p>
 * <p>
 * Also hooks {@link #writeResponse(HttpRequest, HttpResponse, Response)} and
 * {@link #writeException(HttpRequest, HttpResponse, Throwable, Consumer)}, which every completion path
 * (a filter abort, a successful invocation, or a mapped exception) routes through, to finish the Vert.x
 * response exactly once regardless of which path was taken.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class VertxSynchronousDispatcher extends SynchronousDispatcher {

    VertxSynchronousDispatcher(final ResteasyProviderFactory providerFactory) {
        super(providerFactory);
    }

    @Override
    protected void preprocess(final HttpRequest request, final HttpResponse response, final Runnable continuation) {
        super.preprocess(request, response, continuation);
    }

    @Override
    protected void writeResponse(final HttpRequest request, final HttpResponse response, final Response jaxrsResponse) {
        try {
            super.writeResponse(request, response, jaxrsResponse);
        } finally {
            finish(request, response);
        }
    }

    @Override
    public void writeException(final HttpRequest request, final HttpResponse response, final Throwable e,
            final Consumer<Throwable> onComplete) {
        try {
            super.writeException(request, response, e, onComplete);
        } finally {
            finish(request, response);
        }
    }

    private void finish(final HttpRequest request, final HttpResponse response) {
        final VertxHttpRequest vertxRequest = (VertxHttpRequest) request;
        if (vertxRequest.getAsyncContext().isSuspended() || vertxRequest.wasForwarded()) {
            return;
        }
        try {
            ((VertxHttpResponse) response).finish();
        } catch (IOException e) {
            VertxLogger.LOGGER.failedRequest(e);
        }
    }
}
