/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.api;

import io.vertx.core.Vertx;

import dev.resteasy.vertx.VertxManager;

/**
 * Factory for creating {@link Vertx} instances.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}. If no implementation is found, the
 * {@linkplain VertxManager default manager} falls back to {@link Vertx#vertx()}.
 * </p>
 * <p>
 * A custom implementation can be registered by placing a file named
 * {@code META-INF/services/dev.resteasy.vertx.api.VertxFactory} on the classpath containing the
 * fully qualified class name of the implementation. For example, to reuse an existing {@link Vertx} instance:
 * </p>
 *
 * <pre>{@code
 * public class MyVertxFactory implements VertxFactory {
 *     public Vertx create() {
 *         return MyApplication.getSharedVertx();
 *     }
 * }
 * }</pre>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @see VertxManager
 */
@FunctionalInterface
public interface VertxFactory {

    /**
     * Creates or returns a {@link Vertx} instance.
     *
     * @return the Vert.x instance
     */
    Vertx create();
}
