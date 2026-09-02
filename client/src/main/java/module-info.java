/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * RESTEasy Vert.x client module.
 * <p>
 * Provides a RESTEasy client HTTP engine backed by the Eclipse Vert.x HTTP client.
 * </p>
 *
 * @since 2.0
 */
module dev.resteasy.vertx.client {
    // Jakarta EE APIs
    requires jakarta.ws.rs;

    // Third-party dependencies
    // Transitive: exposed in the VertxClientHttpEngine public API (constructors taking HttpClientOptions)
    requires transitive io.vertx.core;
    requires org.jboss.logging;

    // RESTEasy modules
    requires dev.resteasy.vertx;
    requires org.jboss.resteasy.client;
    requires org.jboss.resteasy.core;

    // Exports
    exports dev.resteasy.vertx.client;

    // Provides
    provides org.jboss.resteasy.client.jaxrs.engine.ClientHttpEngineFactory with
            dev.resteasy.vertx.client.VertxClientHttpEngineFactory;
}
