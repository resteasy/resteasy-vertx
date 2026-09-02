/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Internal implementation of the RESTEasy Vert.x embedded server.
 * <p>
 * This package contains the {@link org.jboss.resteasy.plugins.server.embedded.EmbeddedServer} implementation and the
 * adapters bridging Vert.x HTTP requests and responses to the RESTEasy SPI. It is exported only to the CDI module,
 * which extends the server.
 * </p>
 * <p>
 * <strong>This package is not part of the public API and may change without notice.</strong> Users start the server
 * through the standard Jakarta REST {@link jakarta.ws.rs.SeBootstrap} API and customize it through the public SPI in:
 * </p>
 * <ul>
 * <li>{@link dev.resteasy.vertx.server.api} - Router customization SPI</li>
 * </ul>
 *
 * @see dev.resteasy.vertx.server.api
 * @since 2.0
 */
package dev.resteasy.vertx.server;
