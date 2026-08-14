/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.cdi;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.spi.CDI;

import org.jboss.resteasy.plugins.server.embedded.EmbeddedServer;

import io.vertx.ext.web.Route;

import dev.resteasy.server.vertx.VertxEmbeddedServer;

/**
 * A CDI-aware {@link EmbeddedServer} implementation that wraps the {@link VertxEmbeddedServer}.
 * <p>
 * This server integrates CDI with the Vert.x embedded server by:
 * </p>
 * <ul>
 * <li>Automatically booting a CDI container if one is not already running</li>
 * <li>Configuring the RESTEasy deployment to use the {@link org.jboss.resteasy.cdi.CdiInjectorFactory}</li>
 * <li>Registering a request context filter that activates/deactivates the CDI request context per HTTP request</li>
 * </ul>
 * <p>
 * This server is discovered via {@link java.util.ServiceLoader} with a higher priority than the standard
 * {@link VertxEmbeddedServer}, so it is automatically used when the CDI module is on the classpath.
 * </p>
 *
 * @since 2.0
 */
@Priority(100)
public class VertxCdiEmbeddedServer extends VertxEmbeddedServer implements EmbeddedServer {

    /**
     * Creates a new embedded server.
     */
    public VertxCdiEmbeddedServer() {
        super(new CdiResteasyDeployment());
    }

    @Override
    protected void configurePreRoute(final Route route) {
        route.handler(new CdiRequestContextHandler(ManagedSeContainer.instance().lookupBoundRequestContext()));
    }
}
