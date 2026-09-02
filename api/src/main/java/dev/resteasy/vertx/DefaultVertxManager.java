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
import dev.resteasy.vertx.config.ResteasyVertxOptions;
import dev.resteasy.vertx.spi.VertxFactory;

/**
 * Default {@link VertxManager} implementation that creates the {@link Vertx} instance via a {@link VertxFactory}
 * discovered through {@link ServiceLoader}.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class DefaultVertxManager implements VertxManager {
    private final Lock lock = new ReentrantLock();
    private final VertxFactory vertxFactory;
    private int refCounter;
    private Vertx vertx;

    DefaultVertxManager() {
        this.vertxFactory = ServiceLoader.load(VertxFactory.class).findFirst().orElse(Vertx::vertx);
        this.refCounter = 0;
    }

    @Override
    public Vertx vertx() {
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
        return "DefaultVertxManager{vertxFactory=" + vertxFactory +
                ", refCounter=" + refCounter +
                ", vertx=" + vertx +
                '}';
    }
}
