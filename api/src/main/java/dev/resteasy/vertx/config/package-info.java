/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Configuration options for the RESTEasy Vert.x integration.
 * <p>
 * Options are resolved through the RESTEasy {@link org.jboss.resteasy.spi.config.Options} mechanism, so they can be
 * supplied via system properties, environment variables, or any other configuration source RESTEasy supports.
 * </p>
 *
 * <h2>Types</h2>
 * <ul>
 * <li>{@link dev.resteasy.vertx.config.ResteasyVertxOptions ResteasyVertxOptions} - The set of configurable options,
 * including {@link dev.resteasy.vertx.config.ResteasyVertxOptions#TIMEOUT TIMEOUT} and
 * {@link dev.resteasy.vertx.config.ResteasyVertxOptions#TIMEOUT_UNIT TIMEOUT_UNIT} used for blocking operations such
 * as shutting down the {@link io.vertx.core.Vertx} instance</li>
 * </ul>
 *
 * <h2>Example: Overriding the Shutdown Timeout</h2>
 *
 * <pre>
 * -Ddev.resteasy.vertx.timeout=60
 * -Ddev.resteasy.vertx.timeout.unit=SECONDS
 * </pre>
 *
 * @since 2.0
 */
package dev.resteasy.vertx.config;
