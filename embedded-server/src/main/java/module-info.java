/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * RESTEasy Vert.x embedded server module.
 * <p>
 * Provides an embedded Jakarta RESTful Web Services server backed by the Eclipse Vert.x HTTP server.
 * </p>
 *
 * @since 2.0
 */
module dev.resteasy.vertx.server {
    // JDK modules
    // Compile-time only: the jboss-logging processor generates loggers
    requires static java.compiler;

    // Jakarta EE APIs
    requires jakarta.ws.rs;

    // Third-party dependencies
    requires io.vertx.auth.common;
    // Transitive: exposed in the RouterFactory public API (Router create(Vertx vertx))
    requires transitive io.vertx.core;
    requires transitive io.vertx.web;
    requires org.jboss.logging;
    requires static org.jboss.logging.annotations;

    // RESTEasy modules
    requires dev.resteasy.vertx;
    requires org.jboss.resteasy.core;
    requires org.jboss.resteasy.spi;

    // Exports
    exports dev.resteasy.vertx.server to dev.resteasy.vertx.cdi, org.jboss.resteasy.core;
    exports dev.resteasy.vertx.server.api;

    // Opens
    opens dev.resteasy.vertx.server to org.jboss.resteasy.spi;

    // Service consumers
    uses dev.resteasy.vertx.server.api.RouterFactory;

    // Provides
    provides org.jboss.resteasy.plugins.server.embedded.EmbeddedServer with
            dev.resteasy.vertx.server.VertxEmbeddedServer;
}
