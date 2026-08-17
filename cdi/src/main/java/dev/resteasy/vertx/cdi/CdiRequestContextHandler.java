/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.cdi;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.weld.context.bound.BoundRequestContext;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * A Vert.x handler that manages the CDI request context lifecycle around each HTTP request.
 * <p>
 * Uses Weld's {@link BoundRequestContext} backed by an explicit {@link Map} as the bean store, rather than a
 * thread-local. The bean store is stored on the Vert.x {@link Context} so that it is accessible
 * from worker threads via {@code vertx.executeBlocking()}.
 * </p>
 */
class CdiRequestContextHandler implements Handler<RoutingContext> {

    private final BoundRequestContext boundRequestContext;

    CdiRequestContextHandler(final BoundRequestContext boundRequestContext) {
        this.boundRequestContext = Objects.requireNonNull(boundRequestContext,
                "boundRequestContext must not be null");
    }

    @Override
    public void handle(final RoutingContext rc) {
        final Map<String, Object> beanStore = new ConcurrentHashMap<>();
        boundRequestContext.associate(beanStore);
        boundRequestContext.activate();

        final Context vertxContext = Vertx.currentContext();
        if (vertxContext != null) {
            vertxContext.put(CdiVertx.CDI_BEAN_STORE_KEY, beanStore);
        }

        rc.addEndHandler(v -> {
            try {
                try {
                    boundRequestContext.invalidate();
                    boundRequestContext.deactivate();
                } finally {
                    boundRequestContext.dissociate(beanStore);
                }
            } finally {
                if (vertxContext != null) {
                    vertxContext.remove(CdiVertx.CDI_BEAN_STORE_KEY);
                }
            }
        });
        rc.next();
    }
}
