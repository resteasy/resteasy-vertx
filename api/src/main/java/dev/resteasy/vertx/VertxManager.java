/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx;

import java.util.Locale;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.vertx.core.Vertx;

import dev.resteasy.vertx._private.VertxMessages;
import dev.resteasy.vertx.api.VertxFactory;
import dev.resteasy.vertx.config.ResteasyVertxOptions;

/**
 * Manages a shared {@link Vertx} instance across consumers within the same JVM.
 * <p>
 * Each consumer calls {@link #acquire()} to obtain the {@link Vertx} instance and {@link #close()} when finished.
 * The {@link Vertx} instance is created lazily on the first {@code acquire()} and shut down once all consumers
 * have closed.
 * </p>
 * <p>
 * The {@link Vertx} instance is created by a {@link VertxFactory}, discovered via {@link ServiceLoader}. If no
 * factory is found, {@link Vertx#vertx()} is used as the default.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @see VertxFactory
 * @see ResteasyVertxOptions
 */
public final class VertxManager implements AutoCloseable {
    private final Lock lock = new ReentrantLock();
    private final VertxFactory vertxFactory;
    private int refCounter;
    private Vertx vertx;

    private VertxManager(final VertxFactory vertxFactory) {
        this.vertxFactory = vertxFactory;
        this.refCounter = 0;
    }

    /**
     * Returns the {@link VertxManager} instance.
     *
     * @return the manager instance
     */
    public static VertxManager instance() {
        return Holder.INSTANCE;
    }

    /**
     * Returns the shared {@link Vertx} instance, creating it if necessary.
     * <p>
     * Each call to {@code acquire()} must have a corresponding call to {@link #close()}.
     * </p>
     *
     * @return the shared Vert.x instance
     */
    public Vertx acquire() {
        lock.lock();
        try {
            if (vertx == null) {
                vertx = vertxFactory.create();
            }
            refCounter++;
            return vertx;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Releases the {@link Vertx} instance obtained from {@link #acquire()}.
     * <p>
     * The {@link Vertx} instance is shut down once all consumers have closed. If other consumers are still
     * using the instance, this method has no effect on the instance itself.
     * </p>
     *
     * @throws RuntimeException if the {@link Vertx} instance does not shut down within the configured timeout
     * @see ResteasyVertxOptions#TIMEOUT
     * @see ResteasyVertxOptions#TIMEOUT_UNIT
     */
    @Override
    public void close() {
        lock.lock();
        try {
            if (vertx != null) {
                refCounter--;
                if (refCounter == 0) {
                    final long timeout = ResteasyVertxOptions.TIMEOUT.getValue();
                    final TimeUnit unit = ResteasyVertxOptions.TIMEOUT_UNIT.getValue();
                    final Vertx vertx = this.vertx;
                    this.vertx = null;
                    try {
                        vertx.close().await(timeout, unit);
                    } catch (TimeoutException e) {
                        throw VertxMessages.MESSAGES.failedToShutdownVertxWithin(e, timeout,
                                unit.name().toLowerCase(Locale.ROOT));
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        throw VertxMessages.MESSAGES.failedToShutdownVertxWithin(e, timeout,
                                unit.name().toLowerCase(Locale.ROOT));
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return "VertxManager{vertxFactory=" + vertxFactory +
                ", refCounter=" + refCounter +
                ", vertx=" + vertx +
                '}';
    }

    private static class Holder {
        static final VertxManager INSTANCE;

        static {
            final VertxFactory factory = ServiceLoader.load(VertxFactory.class).findFirst().orElse(Vertx::vertx);
            INSTANCE = new VertxManager(factory);
        }
    }
}
