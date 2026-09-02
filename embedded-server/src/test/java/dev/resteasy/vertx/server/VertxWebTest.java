/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.server;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.spi.HttpRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.api.ConfigurationProvider;
import dev.resteasy.vertx.server.spi.RouterFactory;

@RestBootstrap(value = { VertxWebTest.ForwardResource.class, VertxWebTest.TargetResource.class,
        VertxWebTest.PostForwardResource.class, VertxWebTest.PostTargetResource.class,
        VertxWebTest.ContextResource.class,
        VertxWebTest.AttributeResource.class }, configFactory = VertxWebTest.CustomRouterConfigProvider.class)
class VertxWebTest {

    @Test
    void forward(@RestResource @RequestPath("forward") final WebTarget target) {
        try (Response response = target.request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("forwarded-response", response.readEntity(String.class));
        }
    }

    @Test
    void postForward(@RestResource @RequestPath("post-forward") final WebTarget target) {
        try (Response response = target.request().post(Entity.text("hello from post"))) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("echo: hello from post", response.readEntity(String.class));
        }
    }

    @Test
    void routingContextAvailable(@RestResource @RequestPath("context") final WebTarget target) {
        String result = target.request().get(String.class);
        Assertions.assertEquals("pass", result);
    }

    @Test
    void customRouterFactory(@RestResource @RequestPath("target") final WebTarget target) {
        try (Response response = target.request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            Assertions.assertEquals("active", response.getHeaderString("X-Custom-Middleware"));
        }
    }

    @Test
    void middlewareAttributeSharing(@RestResource @RequestPath("attribute") final WebTarget target) {
        String result = target.request().get(String.class);
        Assertions.assertEquals("middleware-value", result);
    }

    @Path("/target")
    public static class TargetResource {
        @GET
        @Produces("text/plain")
        public String target() {
            return "forwarded-response";
        }
    }

    @Path("/forward")
    public static class ForwardResource {
        @GET
        public void forward(@Context final HttpRequest request) {
            request.forward("/target");
        }
    }

    @Path("/post-target")
    public static class PostTargetResource {
        @POST
        @Consumes("text/plain")
        @Produces("text/plain")
        public String postTarget(final String body) {
            return "echo: " + body;
        }
    }

    @Path("/post-forward")
    public static class PostForwardResource {
        @POST
        public void postForward(@Context final HttpRequest request) {
            request.forward("/post-target");
        }
    }

    @Path("/context")
    public static class ContextResource {
        @GET
        @Produces("text/plain")
        public String context() {
            final RoutingContext rc = ResteasyContext.getContextData(RoutingContext.class);
            final Router router = ResteasyContext.getContextData(Router.class);
            if (rc != null && router != null) {
                return "pass";
            }
            return "fail";
        }
    }

    @Path("/attribute")
    public static class AttributeResource {
        @GET
        @Produces("text/plain")
        public String attribute(@Context final HttpRequest request) {
            final Object value = request.getAttribute("X-Middleware-Data");
            return value != null ? value.toString() : "missing";
        }
    }

    public static class CustomRouterConfigProvider implements ConfigurationProvider {
        @Override
        public SeBootstrap.Configuration getConfiguration(final ExtensionContext context) {
            return SeBootstrap.Configuration.builder()
                    .property(RouterFactory.PROPERTY, (RouterFactory) vertx -> {
                        final Router router = Router.router(vertx);
                        router.route().handler(rc -> {
                            rc.response().putHeader("X-Custom-Middleware", "active");
                            rc.data().put("X-Middleware-Data", "middleware-value");
                            rc.next();
                        });
                        return router;
                    })
                    .build();
        }
    }
}
