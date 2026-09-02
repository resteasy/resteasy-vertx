/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.it;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

/**
 * Tests that standard Jakarta REST context types can be injected via {@code @Inject} when CDI is active.
 * These are produced by RESTEasy CDI's {@code ContextProducers}.
 */
@RestBootstrap(value = CdiContextInjectionTest.ContextResource.class)
class CdiContextInjectionTest {

    @Test
    void uriInfo(@RestResource @RequestPath("context/uriInfo") final WebTarget target) {
        try (Response response = target.request().get()) {
            final String value = response.readEntity(String.class);
            Assertions.assertEquals(200, response.getStatus(), () -> "Expected a status of 200: %s".formatted(value));
            Assertions.assertEquals("/context/uriInfo", value);
        }
    }

    @Test
    void httpHeaders(@RestResource @RequestPath("context/httpHeaders") final WebTarget target) {
        try (Response response = target.request().header("X-Test-Header", "test-value").get()) {
            final String value = response.readEntity(String.class);
            Assertions.assertEquals(200, response.getStatus(), () -> "Expected a status of 200: %s".formatted(value));
            Assertions.assertEquals("test-value", value);
        }
    }

    @Test
    void request(@RestResource @RequestPath("context/request") final WebTarget target) {
        try (Response response = target.request().get()) {
            final String value = response.readEntity(String.class);
            Assertions.assertEquals(200, response.getStatus(), () -> "Expected a status of 200: %s".formatted(value));
            Assertions.assertEquals("GET", value);
        }
    }

    @Test
    void securityContext(@RestResource @RequestPath("context/securityContext") final WebTarget target) {
        try (Response response = target.request().get()) {
            final String value = response.readEntity(String.class);
            Assertions.assertEquals(200, response.getStatus(), () -> "Expected a status of 200: %s".formatted(value));
            Assertions.assertEquals("false", value);
        }
    }

    @Test
    void configuration(@RestResource @RequestPath("context/configuration") final WebTarget target) {
        try (Response response = target.request().get()) {
            final String value = response.readEntity(String.class);
            Assertions.assertEquals(200, response.getStatus(), () -> "Expected a status of 200: %s".formatted(value));
            Assertions.assertEquals(RuntimeType.SERVER.name(), value);
        }
    }

    @Test
    void checkVertx(@RestResource @RequestPath("context/vertx") final WebTarget target) {
        try (Response response = target.request().get()) {
            final String value = response.readEntity(String.class);
            Assertions.assertEquals(200, response.getStatus(), () -> "Expected a status of 200: %s".formatted(value));
            Assertions.assertEquals("vertx-injected", value);
        }
    }

    @Test
    void checkRoutingContext(@RestResource @RequestPath("context/routing-context") final WebTarget target) {
        try (Response response = target.request().get()) {
            final String value = response.readEntity(String.class);
            Assertions.assertEquals(200, response.getStatus(), () -> "Expected a status of 200: %s".formatted(value));
            Assertions.assertEquals("routing-context-injected", value);
        }
    }

    @Test
    void checkRouter(@RestResource @RequestPath("context/router") final WebTarget target) {
        try (Response response = target.request().get()) {
            final String value = response.readEntity(String.class);
            Assertions.assertEquals(200, response.getStatus(), () -> "Expected a status of 200: %s".formatted(value));
            Assertions.assertEquals("router-injected", value);
        }
    }

    @Path("/context")
    @Produces(MediaType.TEXT_PLAIN)
    public static class ContextResource {

        @Inject
        UriInfo uriInfo;

        @Inject
        HttpHeaders httpHeaders;

        @Inject
        Request request;

        @Inject
        SecurityContext securityContext;

        @Inject
        Configuration configuration;

        @Inject
        Vertx vertx;

        @Inject
        RoutingContext routingContext;

        @Inject
        Router router;

        @GET
        @Path("/uriInfo")
        public String uriInfo() {
            return uriInfo.getPath();
        }

        @GET
        @Path("/httpHeaders")
        public String httpHeaders() {
            return httpHeaders.getHeaderString("X-Test-Header");
        }

        @GET
        @Path("/request")
        public String request() {
            return request.getMethod();
        }

        @GET
        @Path("/securityContext")
        public String securityContext() {
            return String.valueOf(securityContext.isSecure());
        }

        @GET
        @Path("/configuration")
        public String configuration() {
            return configuration.getRuntimeType().name();
        }

        @GET
        @Path("/vertx")
        public String vertx() {
            return vertx == null ? "vertx-not-injected" : "vertx-injected";
        }

        @GET
        @Path("/routing-context")
        public String routingContext() {
            return routingContext == null ? "routing-context-not-injected" : "routing-context-injected";
        }

        @GET
        @Path("/router")
        public String router() {
            return router == null ? "router-not-injected" : "router-injected";
        }
    }
}
