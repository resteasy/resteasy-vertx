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
public class VertxProducers {

    @Produces
    @Singleton
    public Vertx vertx() {
        return VertxManager.get().vertx();
    }

    @Produces
    @RequestScoped
    public RoutingContext routingContext() {
        final RoutingContext routingContext = ResteasyContext.getContextData(RoutingContext.class);
        if (routingContext == null) {
            throw new IllegalStateException("The RoutingContext could not be found in the RESTEasy context");
        }
        return routingContext;
    }

    @Produces
    @RequestScoped
    public Router router() {
        final Router router = ResteasyContext.getContextData(Router.class);
        if (router == null) {
            throw new IllegalStateException("The Router could not be found in the RESTEasy context");
        }
        return router;
    }

    public void closeVertx(@Disposes final Vertx vertx) {
        VertxManager.get().close();
    }
}
