/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.server.vertx.asyncio;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.sse.SseEventSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

@RestBootstrap(SSEResource.class)
public class SSETest {

    @RestResource
    private Client client;

    @RestResource
    @RequestPath("/close")
    private URI baseUri;

    @Test
    public void testSSE() throws Exception {
        WebTarget target = create("/closed");
        querySSEAndAssert("RESET", "/reset");
        querySSEAndAssert("HELLO", "/send");

        boolean closed = false;
        int cnt = 0;
        while (!closed && cnt < 20) {
            closed = target.request().get(Boolean.class);
            Thread.sleep(200);
            cnt++;
        }

        querySSEAndAssert("CHECK", "/check");
    }

    private void querySSEAndAssert(String message, String uri)
            throws InterruptedException, ExecutionException, TimeoutException {
        WebTarget target = create(uri);
        SseEventSource source = SseEventSource.target(target).build();
        CompletableFuture<String> cf = new CompletableFuture<>();
        source.register(event -> {
            cf.complete(event.readData());
        },
                error -> {
                    cf.completeExceptionally(error);
                },
                () -> {
                    if (!cf.isDone())
                        cf.completeExceptionally(new RuntimeException("closed with no data"));
                });
        source.open();
        try (SseEventSource x = source) {
            Assertions.assertEquals(message, cf.get(5, TimeUnit.SECONDS));
        }
    }

    private WebTarget create(final String path) {
        return client.target(UriBuilder.fromUri(baseUri).path(path).build());
    }
}
