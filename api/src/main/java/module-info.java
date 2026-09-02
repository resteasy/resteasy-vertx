/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * RESTEasy Vert.x API module.
 * <p>
 * Provides the shared API and internal coordination types used by the RESTEasy Vert.x client, server, and CDI
 * integration modules.
 * </p>
 *
 * @since 2.0
 */
module dev.resteasy.vertx {
    // JDK modules
    // Compile-time only: the jboss-logging processor generates loggers
    requires static java.compiler;

    // Jakarta EE APIs
    requires jakarta.ws.rs;

    // Third-party dependencies
    requires transitive io.vertx.core;
    requires io.netty.handler;
    requires org.jboss.logging;
    requires static org.jboss.logging.annotations;

    // RESTEasy modules
    requires org.jboss.resteasy.spi;

    // Exports
    exports dev.resteasy.vertx.spi;
    exports dev.resteasy.vertx.config;
    exports dev.resteasy.vertx.ssl;

    // Internal coordination API shared only with the sibling modules
    exports dev.resteasy.vertx to dev.resteasy.vertx.client, dev.resteasy.vertx.server, dev.resteasy.vertx.cdi;

    // Service consumers
    uses dev.resteasy.vertx.VertxManager;
    uses dev.resteasy.vertx.spi.VertxFactory;
}
