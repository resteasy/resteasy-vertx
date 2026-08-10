/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.server.vertx.async;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.spi.HttpResponseCodes;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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

    /**
     * @tpTestDetails Service unavailable test
     * @tpSince RESTEasy 3.0.16
     */
    @Test
    public void testTimeout(@RestResource @RequestPath("async/timeout") final WebTarget target) throws Exception {
        Response response = target.request().get();
        Assertions.assertEquals(HttpResponseCodes.SC_SERVICE_UNAVAILABLE, response.getStatus());
    }
}
