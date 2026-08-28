/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.server;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Context;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(VertxContextInjectionTest.ContextResource.class)
class VertxContextInjectionTest {

    @Test
    void checkVertx(@RestResource @RequestPath("context/vertx") final WebTarget target) {
        final String value = target.request().get(String.class);
        Assertions.assertEquals("vertx-injected", value);
    }

    @Test
    void checkRoutingContext(@RestResource @RequestPath("context/routing-context") final WebTarget target) {
        final String value = target.request().get(String.class);
        Assertions.assertEquals("routing-context-injected", value);
    }

    @Test
    void checkRouter(@RestResource @RequestPath("context/router") final WebTarget target) {
        final String value = target.request().get(String.class);
        Assertions.assertEquals("router-injected", value);
    }

    @Path("/context")
    public static class ContextResource {
        @Context
        private Vertx vertx;

        @Context
        private RoutingContext routingContext;

        @Context
        private Router router;

        @GET
        @Path("/vertx")
        public String vertx() {
            return vertx == null ? null : "vertx-injected";
        }

        @GET
        @Path("/routing-context")
        public String routingContext() {
            return routingContext == null ? null : "routing-context-injected";
        }

        @GET
        @Path("/router")
        public String router() {
            return router == null ? null : "router-injected";
        }
    }
}
