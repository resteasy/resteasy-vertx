/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.cdi;

import java.util.Locale;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.logging.Logger;
import org.jboss.weld.context.bound.BoundRequestContext;

import io.vertx.core.Vertx;

import dev.resteasy.vertx.VertxManager;
import dev.resteasy.vertx.config.ResteasyVertxOptions;
import dev.resteasy.vertx.spi.VertxFactory;

/**
 * A CDI-aware {@link VertxManager} implementation that wraps the {@link Vertx} instance with CDI context propagation.
 * <p>
 * This manager is discovered via {@link ServiceLoader} with a higher priority than the default manager, so it is
 * automatically used when the CDI module is on the classpath.
 * </p>
 * <p>
 * The boot order is carefully controlled to avoid circular dependencies with CDI:
 * </p>
 * <ol>
 * <li>Create a raw {@link Vertx} instance via {@link VertxFactory}</li>
 * <li>Boot the CDI container (or detect an existing one)</li>
 * <li>Resolve the {@link BoundRequestContext} from the CDI container</li>
 * <li>Wrap the raw {@link Vertx} in a {@link CdiVertx} for context propagation</li>
 * </ol>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 * @since 2.0
 */
public class CdiVertxManager implements VertxManager {
    private static final Logger LOGGER = Logger.getLogger(CdiVertxManager.class);

    private final Lock lock = new ReentrantLock();
    private final VertxFactory vertxFactory;
    private int refCounter;
    private CdiVertx cdiVertx;
    private ManagedSeContainer managedSeContainer;

    /**
     * Creates a new CDI Vert.x manager.
     */
    public CdiVertxManager() {
        this.vertxFactory = ServiceLoader.load(VertxFactory.class).findFirst().orElse(Vertx::vertx);
        this.refCounter = 0;
    }

    @Override
    public Vertx vertx() {
        lock.lock();
        try {
            if (cdiVertx == null) {
                final Vertx rawVertx = vertxFactory.create();
                managedSeContainer = ManagedSeContainer.instance();
                final BoundRequestContext boundRequestContext = managedSeContainer.lookupBoundRequestContext();
                cdiVertx = new CdiVertx(rawVertx, boundRequestContext);
            }
            refCounter++;
            return cdiVertx;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        lock.lock();
        try {
            if (cdiVertx != null) {
                refCounter--;
                if (refCounter == 0) {
                    try {
                        managedSeContainer.shutdown();
                    } catch (Exception e) {
                        LOGGER.warnf(e, "Error shutting down CDI container");
                    }
                    managedSeContainer = null;
                    final long timeout = ResteasyVertxOptions.TIMEOUT.getValue();
                    final TimeUnit unit = ResteasyVertxOptions.TIMEOUT_UNIT.getValue();
                    final CdiVertx vertx = this.cdiVertx;
                    this.cdiVertx = null;
                    try {
                        vertx.close().await(timeout, unit);
                    } catch (TimeoutException e) {
                        throw new RuntimeException(
                                "Failed to shutdown Vertx instance within %d %s".formatted(timeout,
                                        unit.name().toLowerCase(Locale.ROOT)),
                                e);
                    } catch (Exception e) {
                        if (e instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        throw new RuntimeException(
                                "Failed to shutdown Vertx instance within %d %s".formatted(timeout,
                                        unit.name().toLowerCase(Locale.ROOT)),
                                e);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public String toString() {
        return "CdiVertxManager{refCounter=" + refCounter +
                ", cdiVertx=" + cdiVertx +
                '}';
    }
}
