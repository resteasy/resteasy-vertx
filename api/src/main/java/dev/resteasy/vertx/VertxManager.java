/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx;

import java.util.ServiceLoader;

import io.vertx.core.Vertx;

import dev.resteasy.vertx.api.VertxFactory;

/**
 * Manages a shared {@link Vertx} instance across consumers within the same JVM.
 * <p>
 * Each consumer calls {@link #vertx()} to obtain the {@link Vertx} instance and {@link #close()} when finished.
 * The {@link Vertx} instance is created lazily on the first {@code vertx()} and shut down once all consumers
 * have closed.
 * </p>
 * <p>
 * Implementations are discovered via {@link ServiceLoader}. If no implementation is found, a
 * {@linkplain DefaultVertxManager default implementation} is used which creates the {@link Vertx} instance via a
 * {@link VertxFactory}.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @see VertxFactory
 */
public interface VertxManager extends AutoCloseable {

    /**
     * Returns the shared {@link Vertx} instance, creating it if necessary.
     * <p>
     * Each call to {@code vertx()} must have a corresponding call to {@link #close()}.
     * </p>
     *
     * @return the shared Vert.x instance
     */
    Vertx vertx();

    /**
     * Releases the {@link Vertx} instance obtained from {@link #vertx()}.
     * <p>
     * The {@link Vertx} instance is shut down once all consumers have closed. If other consumers are still
     * using the instance, this method has no effect on the instance itself.
     * </p>
     */
    @Override
    void close();

    /**
     * Returns the priority of this manager. Lower values indicate higher priority. The default priority is
     * {@code 1000}.
     *
     * @return the priority value
     */
    default int priority() {
        return 1000;
    }

    /**
     * Returns the {@link VertxManager} instance.
     *
     * @return the manager instance
     */
    static VertxManager get() {
        return VertxManagerHolder.INSTANCE;
    }
}
