/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * RESTEasy Vert.x CDI module.
 * <p>
 * Integrates the RESTEasy Vert.x embedded server with CDI, managing a Weld container and exposing Jakarta RESTful
 * Web Services components as CDI beans.
 * </p>
 *
 * @since 2.0
 */
module dev.resteasy.vertx.cdi {
    // Jakarta EE APIs
    requires jakarta.annotation;
    requires jakarta.cdi;
    requires jakarta.ws.rs;

    // Third-party dependencies
    requires io.vertx.core;
    requires io.vertx.web;
    requires org.jboss.logging;
    // Weld does not yet ship module-info descriptors; these are automatic module names derived from the jars.
    // weld.api and weld.core.impl are transitive: Weld generates client proxies for a consumer's normal-scoped
    // CDI beans into the consumer's own module, and those proxies implement Weld's proxy interfaces
    // (WeldClientProxy in weld.api, ProxyObject in weld.core.impl). Propagating readability here spares every
    // module-path consumer from having to require Weld internals it never references directly.
    requires transitive weld.api;
    requires transitive weld.core.impl;
    requires weld.environment.common;
    requires weld.se.core;

    // RESTEasy modules
    requires dev.resteasy.vertx;
    requires dev.resteasy.vertx.server;
    requires org.jboss.resteasy.cdi;
    requires org.jboss.resteasy.core;
    requires org.jboss.resteasy.spi;

    // Opens
    // Opened so the CDI container (Weld) can reflectively access the beans in this module
    opens dev.resteasy.vertx.cdi;

    // Service consumers
    uses dev.resteasy.vertx.api.VertxFactory;

    // Provides
    provides dev.resteasy.vertx.VertxManager with
            dev.resteasy.vertx.cdi.CdiVertxManager;
    provides org.jboss.resteasy.plugins.server.embedded.EmbeddedServer with
            dev.resteasy.vertx.cdi.VertxCdiEmbeddedServer;
}
