/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Internal coordination API shared among the RESTEasy Vert.x modules.
 * <p>
 * This package holds the types used by the client, server, and CDI modules to share a single
 * {@link io.vertx.core.Vertx} instance within a JVM. It is exported only to those sibling modules.
 * </p>
 * <p>
 * <strong>This package is not part of the public API and may change without notice.</strong> Users customize the
 * integration through the public API in:
 * </p>
 * <ul>
 * <li>{@link dev.resteasy.vertx.spi} - Extension points and SPI</li>
 * <li>{@link dev.resteasy.vertx.config} - Configuration options</li>
 * </ul>
 *
 * @see dev.resteasy.vertx.spi
 * @since 2.0
 */
package dev.resteasy.vertx;
