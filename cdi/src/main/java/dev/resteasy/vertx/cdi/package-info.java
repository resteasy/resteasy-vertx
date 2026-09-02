/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Internal implementation of the RESTEasy Vert.x CDI integration.
 * <p>
 * This package wires the embedded server to a CDI (Weld) container, exposing Jakarta RESTful Web Services components
 * as CDI beans and providing the CDI-aware {@link org.jboss.resteasy.plugins.server.embedded.EmbeddedServer} and
 * {@link dev.resteasy.vertx.VertxManager} implementations discovered via {@link java.util.ServiceLoader}. It is opened
 * to the CDI container for reflective access and is not exported.
 * </p>
 * <p>
 * <strong>This package is not part of the public API and may change without notice.</strong> The integration is
 * activated simply by having this module on the path together with a {@code META-INF/beans.xml}; users interact with
 * it through the standard Jakarta REST {@link jakarta.ws.rs.SeBootstrap} API and CDI.
 * </p>
 *
 * @since 2.0
 */
package dev.resteasy.vertx.cdi;
