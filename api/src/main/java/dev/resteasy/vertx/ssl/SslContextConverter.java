/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.ssl;

import java.util.function.Function;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.spi.tls.SslContextFactory;

/**
 * Bridges a {@link SSLContext} to Vert.x SSL configuration.
 * <p>
 * Vert.x natively expects {@link io.vertx.core.net.KeyCertOptions} and {@link io.vertx.core.net.TrustOptions}
 * for SSL configuration and validates their presence before a server starts. This utility works around that
 * limitation by providing a custom {@link SslContextFactory} that wraps the given {@link SSLContext} into a
 * Netty {@link io.netty.handler.ssl.JdkSslContext}, which already contains all key and trust material.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @since 2.0
 */
public final class SslContextConverter {

    private SslContextConverter() {
    }

    /**
     * Configures SSL on the given server options using the provided {@link SSLContext}.
     *
     * @param options    the HTTP server options to configure
     * @param sslContext the SSL context containing key and trust material
     * @param clientAuth the client authentication mode
     */
    public static void configureSsl(final HttpServerOptions options, final SSLContext sslContext,
            final ClientAuth clientAuth) {
        options.setSsl(true)
                .setUseAlpn(true)
                .setKeyCertOptions(new SslContextKeyCertOptions())
                .setClientAuth(clientAuth)
                .setSslEngineOptions(createEngineOptions(sslContext));
    }

    /**
     * Configures SSL on the given client options using the provided {@link SSLContext}.
     *
     * @param options    the HTTP client options to configure
     * @param sslContext the SSL context containing key and trust material
     */
    public static void configureSsl(final HttpClientOptions options, final SSLContext sslContext) {
        options.setSsl(true)
                .setSslEngineOptions(createEngineOptions(sslContext));
    }

    private static JdkSSLEngineOptions createEngineOptions(final SSLContext sslContext) {
        return new JdkSSLEngineOptions() {
            @Override
            public SslContextFactory sslContextFactory() {
                return new JdkSslContextFactory(sslContext);
            }

            @Override
            public JdkSSLEngineOptions copy() {
                return this;
            }
        };
    }

    /**
     * A no-op {@link KeyCertOptions} that satisfies the Vert.x server validation requiring non-null key/cert
     * options when SSL is enabled. The actual key material is provided by the {@link SSLContext} via the
     * {@link JdkSslContextFactory}.
     */
    private static class SslContextKeyCertOptions implements KeyCertOptions {

        @Override
        public KeyCertOptions copy() {
            return this;
        }

        @Override
        public KeyManagerFactory getKeyManagerFactory(final Vertx vertx) {
            return null;
        }

        @Override
        public Function<String, KeyManagerFactory> keyManagerFactoryMapper(final Vertx vertx) {
            return null;
        }
    }
}
