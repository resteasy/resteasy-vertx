/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

/**
 * Exercises the CDI integration end-to-end on the module path.
 */
@RestBootstrap(value = CdiModulePathTest.CdiResource.class)
class CdiModulePathTest {

    @Test
    void applicationScopedInjection(@RestResource @RequestPath("cdi/application-scoped") final WebTarget target) {
        assertEquals(1, target.request().get(int.class), "Expected a count of 1");
        assertEquals(2, target.request().get(int.class), "Expected a count of 2");
    }

    @Test
    void vertxInjection(@RestResource @RequestPath("cdi/vertx") final WebTarget target) {
        assertEquals("vertx-available", target.request().get(String.class),
                "The Vert.x instance was not injected via CDI");
    }

    @Test
    void requestScopedIsolation(@RestResource @RequestPath("cdi/request-scoped") final WebTarget target) {
        // Each request gets a fresh @RequestScoped bean, so the counter always starts fresh
        assertEquals(1, target.request().get(int.class), "First request should see a fresh counter");
        assertEquals(1, target.request().get(int.class), "RequestScoped bean should be fresh per request");
    }

    @ApplicationScoped
    public static class ApplicationScopedCounter {
        private final AtomicInteger counter = new AtomicInteger();

        public int incrementAndGet() {
            return counter.incrementAndGet();
        }
    }

    @RequestScoped
    public static class RequestScopedCounter {
        private final AtomicInteger counter = new AtomicInteger();

        public int incrementAndGet() {
            return counter.incrementAndGet();
        }
    }

    @Path("/cdi")
    public static class CdiResource {

        @Inject
        ApplicationScopedCounter greetingService;

        @Inject
        RequestScopedCounter counter;

        @Inject
        Vertx vertx;

        @GET
        @Path("/application-scoped")
        @Produces(MediaType.TEXT_PLAIN)
        public int applicationScoped() {
            return greetingService.incrementAndGet();
        }

        @GET
        @Path("/request-scoped")
        @Produces(MediaType.TEXT_PLAIN)
        public int requestScoped() {
            return counter.incrementAndGet();
        }

        @GET
        @Path("/vertx")
        @Produces(MediaType.TEXT_PLAIN)
        public String vertxInjected() {
            return vertx != null ? "vertx-available" : "vertx-missing";
        }
    }
}
