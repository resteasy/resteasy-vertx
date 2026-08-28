/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.server.asyncio;

import jakarta.ws.rs.client.WebTarget;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

@RestBootstrap({
        MyTypeWriter.class,
        MyTypeInterceptor.class,
        AsyncIOWriterResource.class
})
public class AsyncIOWriterTest {

    @Test
    public void testAsyncIoWriter(@RestResource @RequestPath("/async-io-writer") final WebTarget target) throws Exception {
        String val = target.request().get(String.class);
        Assertions.assertEquals("OK", val);
    }
}
