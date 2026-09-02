/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * SSL/TLS support bridging a JDK {@link javax.net.ssl.SSLContext} to Vert.x SSL configuration.
 * <p>
 * Vert.x natively expects {@link io.vertx.core.net.KeyCertOptions} and {@link io.vertx.core.net.TrustOptions} and
 * validates their presence before a server or client starts. The types in this package allow a standard
 * {@link javax.net.ssl.SSLContext} - the form the Jakarta REST {@code SeBootstrap} API exposes - to be used instead,
 * by wrapping it into a Netty {@code JdkSslContext} that already carries all key and trust material.
 * </p>
 *
 * <h2>Types</h2>
 * <ul>
 * <li>{@link dev.resteasy.vertx.ssl.SslContextConverter SslContextConverter} - Configures SSL on Vert.x
 * {@link io.vertx.core.http.HttpServerOptions} or {@link io.vertx.core.http.HttpClientOptions} from a given
 * {@link javax.net.ssl.SSLContext}</li>
 * </ul>
 *
 * @since 2.0
 */
package dev.resteasy.vertx.ssl;
