/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.cdi;

import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.se.SeContainer;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.server.embedded.EmbeddedServer;
import org.jboss.resteasy.plugins.server.embedded.EmbeddedServers;
import org.jboss.weld.environment.se.Weld;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;

/**
 * Tests for CDI integration with the Vert.x embedded server.
 */
class StandaloneCdiTest {
    private static final Logger LOGGER = Logger.getLogger(StandaloneCdiTest.class);

    private static SeContainer SE_CONTAINER;
    private static EmbeddedServer EMBEDDED_SERVER;
    private static Client CLIENT;
    private static URI BASE_URI;

    @BeforeAll
    static void setupContainers() {
        final Weld weld = new Weld()
                .skipShutdownHook();
        SE_CONTAINER = weld.initialize();
        EMBEDDED_SERVER = EmbeddedServers.findServer();
        final SeBootstrap.Configuration configuration = SeBootstrap.Configuration.builder().build();
        EMBEDDED_SERVER.getDeployment().getActualResourceClasses().add(CdiResource.class);
        EMBEDDED_SERVER.start(configuration);
        CLIENT = ClientBuilder.newClient();
        BASE_URI = configuration.baseUri();
    }

    @AfterAll
    static void shutdownContainers() {
        safeClose(CLIENT);
        safeStop(EMBEDDED_SERVER);
        safeClose(SE_CONTAINER);
    }

    @Test
    void cdiInjection() {
        final WebTarget target = CLIENT.target(BASE_URI).path("cdi/greeting");
        final String result = target.request().get(String.class);
        Assertions.assertEquals("Hello from CDI", result);
    }

    @Test
    void requestScopedIsolation() {
        final WebTarget target = CLIENT.target(BASE_URI).path("cdi/request-scoped");
        // Each request gets a fresh @RequestScoped bean, so counter always starts at 0
        final int first = target.request().get(int.class);
        Assertions.assertEquals(1, first);

        final int second = target.request().get(int.class);
        Assertions.assertEquals(1, second, "RequestScoped bean should be fresh per request");
    }

    @Test
    void vertxInjection() {
        final WebTarget target = CLIENT.target(BASE_URI).path("cdi/vertx");
        final String result = target.request().get(String.class);
        Assertions.assertEquals("vertx-available", result);
    }

    @Test
    void responseStatus() {
        final WebTarget target = CLIENT.target(BASE_URI).path("cdi/greeting");
        try (Response response = target.request().get()) {
            Assertions.assertEquals(200, response.getStatus());
        }
    }

    private static void safeClose(final AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                LOGGER.errorf(e, "Exception while closing %s", closeable);
            }
        }
    }

    private static void safeStop(final EmbeddedServer server) {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                LOGGER.errorf(e, "Exception while closing %s", server);
            }
        }
    }

    @ApplicationScoped
    public static class GreetingService {
        public String greet() {
            return "Hello from CDI";
        }
    }

    @RequestScoped
    public static class RequestScopedCounter {
        private int count;

        public int increment() {
            return ++count;
        }
    }

    @Path("/cdi")
    @RequestScoped
    public static class CdiResource {

        @Inject
        GreetingService greetingService;

        @Inject
        RequestScopedCounter counter;

        @Inject
        Vertx vertx;

        @GET
        @Path("/greeting")
        @Produces(MediaType.TEXT_PLAIN)
        public String greeting() {
            return greetingService.greet();
        }

        @GET
        @Path("/request-scoped")
        @Produces(MediaType.TEXT_PLAIN)
        public int requestScoped() {
            return counter.increment();
        }

        @GET
        @Path("/vertx")
        @Produces(MediaType.TEXT_PLAIN)
        public String vertxInjected() {
            return vertx != null ? "vertx-available" : "vertx-missing";
        }
    }
}
