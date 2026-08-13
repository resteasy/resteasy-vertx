/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.server.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;
import dev.resteasy.junit.extension.annotations.SelfSignedCert;

@RestBootstrap(value = VertxSslTest.SslResource.class)
@SelfSignedCert
class VertxSslTest {

    @Test
    void testHttps(@RestResource @RequestPath("/ssl/hello") final WebTarget target) {
        assertEquals("https", target.getUri().getScheme());
        try (Response response = target.request().get()) {
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void testSecurityContextIsSecure(@RestResource @RequestPath("/ssl/secure") final WebTarget target) {
        try (Response response = target.request().get()) {
            assertEquals(200, response.getStatus());
            assertTrue(response.readEntity(Boolean.class), "SecurityContext.isSecure() should return true for HTTPS");
        }
    }

    @Path("/ssl")
    public static class SslResource {
        @GET
        @Path("/hello")
        @Produces("text/plain")
        public String hello() {
            return "hello ssl";
        }

        @GET
        @Path("/secure")
        @Produces("text/plain")
        public String isSecure(@Context SecurityContext securityContext) {
            return String.valueOf(securityContext.isSecure());
        }
    }
}
