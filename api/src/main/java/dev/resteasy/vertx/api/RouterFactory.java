/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.api;

import jakarta.ws.rs.SeBootstrap;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

import dev.resteasy.vertx.VertxManager;

/**
 * Factory for creating {@link Router} instances.
 * <p>
 * A factory is resolved in the following order:
 * </p>
 * <ol>
 * <li>A {@code RouterFactory} set on the {@link SeBootstrap.Configuration} via the {@link #PROPERTY} property</li>
 * <li>A {@code RouterFactory} discovered via {@link java.util.ServiceLoader}</li>
 * <li>A default factory that creates a plain {@link Router} via {@link Router#router(Vertx)}</li>
 * </ol>
 *
 * <h2>Configuration property</h2>
 * <p>
 * A factory can be provided per-startup via {@link SeBootstrap.Configuration}. Since {@code RouterFactory} is a
 * {@link FunctionalInterface}, a lambda can be used:
 * </p>
 *
 * <pre>{@code
 * Configuration config = Configuration.builder()
 *         .property(RouterFactory.ROUTER_FACTORY, (RouterFactory) vertx -> {
 *             Router router = Router.router(vertx);
 *             router.route().handler(CorsHandler.create());
 *             return router;
 *         })
 *         .build();
 * SeBootstrap.start(MyApplication.class, config);
 * }</pre>
 *
 * <h2>ServiceLoader</h2>
 * <p>
 * A global default factory can be registered by placing a file named
 * {@code META-INF/services/dev.resteasy.vertx.api.RouterFactory} on the classpath containing the
 * fully qualified class name of the implementation:
 * </p>
 *
 * <pre>{@code
 * public class MyRouterFactory implements RouterFactory {
 *     public Router create(Vertx vertx) {
 *         Router router = Router.router(vertx);
 *         router.route().handler(CorsHandler.create());
 *         return router;
 *     }
 * }
 * }</pre>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @see VertxManager
 */
@FunctionalInterface
public interface RouterFactory {

    /**
     * A property name used to reference a {@link RouterFactory} placed in the {@linkplain SeBootstrap.Configuration
     * configuration}.
     */
    String PROPERTY = "dev.resteasy.vertx.server.router.factory";

    /**
     * Creates or returns a {@link Router} instance.
     *
     * @param vertx the Vert.x instance
     * @return the router
     */
    Router create(Vertx vertx);
}
