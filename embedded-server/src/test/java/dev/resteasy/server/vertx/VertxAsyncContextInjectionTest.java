/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.server.vertx;

import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

/**
 * Tests that {@code @Context}-ed Vert.x context types remain accessible inside async callbacks ({@link CompletionStage}
 * and {@link AsyncResponse}).
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@RestBootstrap(value = VertxAsyncContextInjectionTest.AsyncContextResource.class)
class VertxAsyncContextInjectionTest {

    @Test
    void completionStageVertx(@RestResource @RequestPath("async-context/cs/vertx") final WebTarget target) {
        try (Response response = target.request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("vertx-injected", response.readEntity(String.class));
        }
    }

    @Test
    void completionStageRoutingContext(
            @RestResource @RequestPath("async-context/cs/routing-context") final WebTarget target) {
        try (Response response = target.request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("routing-context-injected", response.readEntity(String.class));
        }
    }

    @Test
    void completionStageRouter(@RestResource @RequestPath("async-context/cs/router") final WebTarget target) {
        try (Response response = target.request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("router-injected", response.readEntity(String.class));
        }
    }

    @Test
    void asyncResponseVertx(@RestResource @RequestPath("async-context/ar/vertx") final WebTarget target) {
        try (Response response = target.request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("vertx-injected", response.readEntity(String.class));
        }
    }

    @Test
    void asyncResponseRoutingContext(
            @RestResource @RequestPath("async-context/ar/routing-context") final WebTarget target) {
        try (Response response = target.request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("routing-context-injected", response.readEntity(String.class));
        }
    }

    @Test
    void asyncResponseRouter(@RestResource @RequestPath("async-context/ar/router") final WebTarget target) {
        try (Response response = target.request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("router-injected", response.readEntity(String.class));
        }
    }

    @Path("/async-context")
    @Produces(MediaType.TEXT_PLAIN)
    public static class AsyncContextResource {

        @Context
        Vertx vertx;

        @Context
        RoutingContext routingContext;

        @Context
        Router router;

        @GET
        @Path("/cs/vertx")
        public CompletionStage<String> csVertx() {
            return vertx.executeBlocking(() -> vertx == null ? null : "vertx-injected").toCompletionStage();
        }

        @GET
        @Path("/cs/routing-context")
        public CompletionStage<String> csRoutingContext() {
            return vertx
                    .executeBlocking(() -> routingContext == null ? null : "routing-context-injected")
                    .toCompletionStage();
        }

        @GET
        @Path("/cs/router")
        public CompletionStage<String> csRouter() {
            return vertx.executeBlocking(() -> router == null ? null : "router-injected").toCompletionStage();
        }

        @GET
        @Path("/ar/vertx")
        public void arVertx(@Suspended final AsyncResponse asyncResponse) {
            vertx.executeBlocking(() -> vertx == null ? null : "vertx-injected", false)
                    .onSuccess(asyncResponse::resume);
        }

        @GET
        @Path("/ar/routing-context")
        public void arRoutingContext(@Suspended final AsyncResponse asyncResponse) {
            vertx.executeBlocking(() -> routingContext == null ? null : "routing-context-injected", false)
                    .onSuccess(asyncResponse::resume);
        }

        @GET
        @Path("/ar/router")
        public void arRouter(@Suspended final AsyncResponse asyncResponse) {
            vertx.executeBlocking(() -> router == null ? null : "router-injected", false)
                    .onSuccess(asyncResponse::resume);
        }
    }
}
