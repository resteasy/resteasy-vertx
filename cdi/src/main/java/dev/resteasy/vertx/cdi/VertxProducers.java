/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.jboss.resteasy.core.ResteasyContext;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import dev.resteasy.vertx.VertxManager;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@ApplicationScoped
class VertxProducers {

    @Produces
    @Singleton
    Vertx vertx() {
        return VertxManager.get().vertx();
    }

    @Produces
    @RequestScoped
    // Return a concrete type within this package. See the Javadoc on the delegate for details.
    DelegateRoutingContext routingContext() {
        final RoutingContext routingContext = ResteasyContext.getContextData(RoutingContext.class);
        if (routingContext == null) {
            throw new IllegalStateException("The RoutingContext could not be found in the RESTEasy context");
        }
        return new DelegateRoutingContext(routingContext);
    }

    @Produces
    @RequestScoped
    // Return a concrete type within this package. See the Javadoc on the delegate for details.
    DelegateRouter router() {
        final Router router = ResteasyContext.getContextData(Router.class);
        if (router == null) {
            throw new IllegalStateException("The Router could not be found in the RESTEasy context");
        }
        return new DelegateRouter(router);
    }

    void closeVertx(@Disposes final Vertx vertx) {
        VertxManager.get().close();
    }
}
