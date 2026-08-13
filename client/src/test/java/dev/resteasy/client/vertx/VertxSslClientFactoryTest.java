/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.client.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

import dev.resteasy.junit.extension.annotations.SelfSignedCert;
import dev.resteasy.junit.extension.annotations.SslCert;
import dev.resteasy.junit.extension.api.SelfSignedCertificate;
import dev.resteasy.vertx.ssl.SslContextConverter;

/**
 * Tests that the {@link VertxClientHttpEngineFactory} correctly forwards SSL configuration from
 * {@link jakarta.ws.rs.client.ClientBuilder#sslContext(SSLContext)} to the Vert.x HTTP client.
 */
@SelfSignedCert
class VertxSslClientFactoryTest {
    @SslCert
    private SelfSignedCertificate certificate;
    private Vertx vertx;
    private HttpServer server;

    @BeforeEach
    void before() throws Exception {
        vertx = Vertx.vertx();
        final HttpServerOptions serverOptions = new HttpServerOptions();
        SslContextConverter.configureSsl(serverOptions, certificate.serverSslContext(), ClientAuth.REQUIRED);
        server = vertx.createHttpServer(serverOptions);
        server.requestHandler(req -> req.response().end("ssl-ok"));
        final CompletableFuture<Void> fut = new CompletableFuture<>();
        server.listen(0).onComplete(ar -> {
            if (ar.succeeded()) {
                fut.complete(null);
            } else {
                fut.completeExceptionally(ar.cause());
            }
        });
        fut.get(2, TimeUnit.MINUTES);
    }

    @AfterEach
    void stop() {
        server.close().toCompletionStage().toCompletableFuture().join();
        vertx.close().toCompletionStage().toCompletableFuture().join();
    }

    @Test
    void testSslContextViaClientBuilder() {
        try (Client client = ClientBuilder.newBuilder()
                .sslContext(certificate.clientSslContext())
                .build()) {
            final URI uri = URI.create("https://localhost:" + server.actualPort());
            final Response resp = client.target(uri).request().get();
            assertEquals(200, resp.getStatus());
            assertEquals("ssl-ok", resp.readEntity(String.class));
        }
    }
}
