/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.HttpVersion;

import dev.resteasy.junit.extension.annotations.SelfSignedCert;
import dev.resteasy.junit.extension.annotations.SslCert;
import dev.resteasy.junit.extension.api.SelfSignedCertificate;
import dev.resteasy.vertx.ssl.SslContextConverter;

@SelfSignedCert
public class VertxSslClientEngineTest {
    @SslCert
    private SelfSignedCertificate certificate;
    private Vertx vertx;
    private HttpServer server;
    private ScheduledExecutorService executorService;

    @BeforeEach
    public void before() throws Exception {
        vertx = Vertx.vertx();
        final HttpServerOptions serverOptions = new HttpServerOptions()
                .setSsl(true)
                .setUseAlpn(true);
        SslContextConverter.configureSsl(serverOptions, certificate.serverSslContext(), ClientAuth.REQUIRED);
        server = vertx.createHttpServer(serverOptions);
        executorService = Executors.newSingleThreadScheduledExecutor();
        server.requestHandler(req -> {
            final HttpServerResponse response = req.response();
            if (req.getHeader("User-Agent").contains("Apache")) {
                response.setStatusCode(503).end();
            } else {
                response.end("Success " + req.version().alpnName());
            }
        });
        if (server.actualPort() == 0) {
            CompletableFuture<Void> fut = new CompletableFuture<>();
            server.listen(0).onComplete(ar -> {
                if (ar.succeeded()) {
                    fut.complete(null);
                } else {
                    fut.completeExceptionally(ar.cause());
                }
            });
            fut.get(2, TimeUnit.MINUTES);
        }
    }

    @AfterEach
    public void stop() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        vertx.close().onComplete(ar -> latch.countDown());
        latch.await(2, TimeUnit.MINUTES);
        executorService.shutdownNow();
    }

    @Test
    public void testHTTPS() throws Exception {
        try (Client client = createClient(new HttpClientOptions())) {
            final Response resp = client.target(baseUri()).request().get();
            assertEquals(200, resp.getStatus());
            assertEquals("Success http/1.1", resp.readEntity(String.class));
        }
    }

    @Test
    public void testHTTP2() throws Exception {
        final HttpClientOptions options = new HttpClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setUseAlpn(true);
        try (Client client = createClient(options)) {
            final Response resp = client.target(baseUri()).request().get();
            assertEquals(200, resp.getStatus());
            assertEquals("Success h2", resp.readEntity(String.class));
        }
    }

    private URI baseUri() {
        return URI.create("https://localhost:" + server.actualPort());
    }

    private Client createClient(final HttpClientOptions options) {
        options.setSsl(true);
        SslContextConverter.configureSsl(options, certificate.clientSslContext());
        return ClientBuilder.newBuilder()
                .scheduledExecutorService(executorService)
                .property(VertxClientProperties.HTTP_CLIENT_OPTIONS, options)
                .build();
    }
}
