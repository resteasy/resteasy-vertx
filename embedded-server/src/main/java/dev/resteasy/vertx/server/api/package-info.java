/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Public SPI for customizing the RESTEasy Vert.x embedded server.
 * <p>
 * The embedded server routes requests through a Vert.x Web {@link io.vertx.ext.web.Router}. This package lets users
 * supply their own router so they can add Vert.x Web middleware such as CORS handlers, session management, or
 * authentication.
 * </p>
 *
 * <h2>SPI Interfaces</h2>
 * <ul>
 * <li>{@link dev.resteasy.vertx.server.api.RouterFactory RouterFactory} - Creates the
 * {@link io.vertx.ext.web.Router} used by the server</li>
 * </ul>
 *
 * <h2>Registration</h2>
 * <p>
 * A {@link dev.resteasy.vertx.server.api.RouterFactory} may be supplied per startup via the
 * {@link jakarta.ws.rs.SeBootstrap.Configuration} property
 * {@link dev.resteasy.vertx.server.api.RouterFactory#PROPERTY}, or globally via Java's
 * {@link java.util.ServiceLoader}. When neither is present the server uses a plain
 * {@link io.vertx.ext.web.Router#router(io.vertx.core.Vertx)}.
 * </p>
 *
 * <h2>Example: Per-Startup Router Factory</h2>
 *
 * <pre>
 * SeBootstrap.Configuration config = SeBootstrap.Configuration.builder()
 *         .property(RouterFactory.PROPERTY, (RouterFactory) vertx -&gt; {
 *             Router router = Router.router(vertx);
 *             router.route().handler(CorsHandler.create());
 *             return router;
 *         })
 *         .build();
 * SeBootstrap.start(MyApplication.class, config);
 * </pre>
 *
 * @since 2.0
 */
package dev.resteasy.vertx.server.api;
