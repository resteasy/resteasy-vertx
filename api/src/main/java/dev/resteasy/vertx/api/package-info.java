/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Public API and SPI shared by the RESTEasy Vert.x integration modules.
 * <p>
 * This package contains the service provider interfaces users implement to customize how the RESTEasy Vert.x client
 * and server obtain their {@link io.vertx.core.Vertx} instance.
 * </p>
 *
 * <h2>SPI Interfaces</h2>
 * <ul>
 * <li>{@link dev.resteasy.vertx.api.VertxFactory VertxFactory} - Creates or supplies the {@link io.vertx.core.Vertx}
 * instance used by the integration, for example to reuse an application-managed instance</li>
 * </ul>
 *
 * <h2>Service Registration</h2>
 * <p>
 * A {@link dev.resteasy.vertx.api.VertxFactory} implementation is discovered via Java's
 * {@link java.util.ServiceLoader} by adding a
 * {@code META-INF/services/dev.resteasy.vertx.api.VertxFactory} file containing the fully-qualified implementation
 * class name. If none is found, the integration falls back to {@link io.vertx.core.Vertx#vertx()}.
 * </p>
 *
 * <h2>Example: Reusing an Existing Vertx Instance</h2>
 *
 * <pre>
 * public class MyVertxFactory implements VertxFactory {
 *     &#64;Override
 *     public Vertx create() {
 *         return MyApplication.getSharedVertx();
 *     }
 * }
 * </pre>
 *
 * @since 2.0
 */
package dev.resteasy.vertx.api;
