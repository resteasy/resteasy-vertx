/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * A RESTEasy client HTTP engine backed by the Eclipse Vert.x HTTP client.
 * <p>
 * The engine executes Jakarta REST client requests asynchronously on Vert.x's event-driven architecture, streaming
 * request bodies with chunked transfer encoding when no explicit {@code Content-Length} is provided. It is discovered
 * automatically via {@link java.util.ServiceLoader} as a
 * {@link org.jboss.resteasy.client.jaxrs.engine.ClientHttpEngineFactory}, so simply having this module on the path is
 * enough for the RESTEasy client to use it.
 * </p>
 *
 * <h2>Types</h2>
 * <ul>
 * <li>{@link dev.resteasy.vertx.client.VertxClientHttpEngine VertxClientHttpEngine} - The Vert.x-backed
 * {@link org.jboss.resteasy.client.jaxrs.engines.AsyncClientHttpEngine} implementation</li>
 * <li>{@link dev.resteasy.vertx.client.VertxClientHttpEngineFactory VertxClientHttpEngineFactory} - The service
 * factory that creates the engine from the RESTEasy client configuration</li>
 * <li>{@link dev.resteasy.vertx.client.VertxClientProperties VertxClientProperties} - Property names for tuning the
 * engine, such as a per-request timeout or custom {@link io.vertx.core.http.HttpClientOptions}</li>
 * </ul>
 *
 * <h2>Example: Custom HttpClientOptions</h2>
 *
 * <pre>
 * Client client = ClientBuilder.newBuilder()
 *         .property(VertxClientProperties.HTTP_CLIENT_OPTIONS, new HttpClientOptions().setHttp2ClearTextUpgrade(false))
 *         .build();
 * </pre>
 *
 * @since 2.0
 */
package dev.resteasy.vertx.client;
