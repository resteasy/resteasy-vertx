/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.vertx.ext.web.Router;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.api.ConfigurationProvider;
import dev.resteasy.vertx.server.spi.RouterFactory;

/**
 * Simple smoke test to ensure the module descriptors work as expected.
 */
@RestBootstrap(value = EmbeddedServerModulePathTest.HelloResource.class, configFactory = EmbeddedServerModulePathTest.RouterConfigurationProvider.class)
class EmbeddedServerModulePathTest {

    @Test
    void customRouterFactoryAppliedOnModulePath(@RestResource @RequestPath("/hello") final WebTarget target) {
        try (Response response = target.request().get()) {

            assertEquals(200, response.getStatus(), "Unexpected status code");
            assertEquals("hello", response.readEntity(String.class), "Unexpected response body");
            assertEquals("applied", response.getHeaderString("X-Integration-Test"),
                    "The custom RouterFactory header is missing, so the RouterFactory was not applied");
        }
    }

    @Path("/hello")
    public static class HelloResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String hello() {
            return "hello";
        }
    }

    public static class RouterConfigurationProvider implements ConfigurationProvider {

        @Override
        public SeBootstrap.Configuration getConfiguration(final ExtensionContext context) {
            return SeBootstrap.Configuration.builder()
                    // Use an ephemeral port so the test never clashes with a bound port in CI
                    .port(SeBootstrap.Configuration.FREE_PORT)
                    .property(RouterFactory.PROPERTY, (RouterFactory) vertx -> {
                        final Router router = Router.router(vertx);
                        router.route().handler(rc -> {
                            rc.response().putHeader("X-Integration-Test", "applied");
                            rc.next();
                        });
                        return router;
                    })
                    .build();
        }
    }
}
