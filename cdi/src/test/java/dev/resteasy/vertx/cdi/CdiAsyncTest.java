/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.cdi;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

/**
 * Tests that {@code @RequestScoped} CDI beans work correctly with async resource methods
 * returning {@link CompletionStage}.
 */
@RestBootstrap(value = CdiAsyncTest.AsyncResource.class)
class CdiAsyncTest {

    @Test
    void asyncWithRequestScoped(@RestResource @RequestPath("async/greeting") final WebTarget target) {
        final String result = target.request().get(String.class);
        Assertions.assertEquals("Async Hello from CDI", result);
    }

    @Test
    void asyncCounter(@RestResource @RequestPath("async/async-counter") final WebTarget target) {
        final int first = target.request().get(int.class);
        Assertions.assertEquals(1, first);

        final int second = target.request().get(int.class);
        Assertions.assertEquals(1, second, "RequestScoped bean should be fresh per async request");
    }

    @Test
    void asyncRequestScopedIsolation(@RestResource @RequestPath("async/counter") final WebTarget target) {
        final int first = target.request().get(int.class);
        Assertions.assertEquals(1, first);

        final int second = target.request().get(int.class);
        Assertions.assertEquals(1, second, "RequestScoped bean should be fresh per async request");
    }

    @RequestScoped
    public static class AsyncGreetingService {
        public String greet() {
            return "Async Hello from CDI";
        }
    }

    @RequestScoped
    public static class AsyncCounter {
        private int count;

        public int increment() {
            return ++count;
        }
    }

    @Path("/async")
    public static class AsyncResource {

        @Inject
        Vertx vertx;

        @Inject
        AsyncGreetingService greetingService;

        @Inject
        AsyncCounter counter;

        @GET
        @Path("/greeting")
        @Produces(MediaType.TEXT_PLAIN)
        public CompletionStage<String> greeting() {
            return vertx.executeBlocking(() -> greetingService.greet()).toCompletionStage();
        }

        @GET
        @Path("/counter")
        @Produces(MediaType.TEXT_PLAIN)
        public CompletionStage<Integer> counter() {
            return vertx.executeBlocking(() -> counter.increment()).toCompletionStage();
        }

        @GET
        @Path("/async-counter")
        @Produces(MediaType.TEXT_PLAIN)
        public void asyncCounter(@Suspended final AsyncResponse asyncResponse) {
            final WorkerExecutor executor = vertx.createSharedWorkerExecutor("test");
            try {
                executor.executeBlocking(() -> asyncResponse.resume(counter.increment()));
            } finally {
                executor.close();
            }
        }
    }
}
