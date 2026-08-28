/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.server.async;

import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.spi.HttpResponseCodes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

@RestBootstrap(AsyncResource.class)
public class AsyncTest {

    /**
     * @tpTestDetails Test for correct response
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testAsync(@RestResource @RequestPath("async") final WebTarget target) throws Exception {
        Response response = target.request().get();
        Assertions.assertEquals(HttpResponseCodes.SC_OK, response.getStatus());
        Assertions.assertEquals("hello", response.readEntity(String.class), "Wrong response content");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    public void testExceptionWhileSuspended(@RestResource @RequestPath("async/throw") final WebTarget target) {
        Response response = target.request().get();
        Assertions.assertEquals(Response.Status.INTERNAL_SERVER_ERROR, response.getStatusInfo());
    }

    @Test
    public void testTimeout(@RestResource @RequestPath("async/timeout") final WebTarget target) throws Exception {
        Response response = target.request().get();
        Assertions.assertEquals(HttpResponseCodes.SC_SERVICE_UNAVAILABLE, response.getStatus());
    }
}
