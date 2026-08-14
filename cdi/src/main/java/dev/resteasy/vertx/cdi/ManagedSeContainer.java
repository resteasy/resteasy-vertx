/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.cdi;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.enterprise.inject.se.SeContainer;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;
import org.jboss.weld.context.bound.BoundLiteral;
import org.jboss.weld.context.bound.BoundRequestContext;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;

/**
 * Manages the lifecycle of the CDI {@link SeContainer}.
 * <p>
 * The container is initialized lazily via {@link #instance()} and shut down via {@link #shutdown()}. If an existing
 * Weld container is detected, it is reused without managing its lifecycle.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class ManagedSeContainer {
    private static final Logger LOGGER = Logger.getLogger(ManagedSeContainer.class);
    private static final Lock LOCK = new ReentrantLock();
    private static ManagedSeContainer INSTANCE;

    private final SeContainer container;
    private final boolean managed;

    private ManagedSeContainer(SeContainer container, boolean managed) {
        this.container = container;
        this.managed = managed;
    }

    static ManagedSeContainer instance() {
        LOCK.lock();
        try {
            if (INSTANCE == null) {
                INSTANCE = initialize();
            }
            return INSTANCE;
        } finally {
            LOCK.unlock();
        }
    }

    void shutdown() {
        try {
            if (managed) {
                container.close();
            }
        } finally {
            LOCK.lock();
            try {
                INSTANCE = null;
            } finally {
                LOCK.unlock();
            }
        }
    }

    BeanManager getBeanManager() {
        return container.getBeanManager();
    }

    SeContainer container() {
        return container;
    }

    boolean managed() {
        return managed;
    }

    BoundRequestContext lookupBoundRequestContext() {
        return container.select(BoundRequestContext.class, BoundLiteral.INSTANCE).get();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof ManagedSeContainer other)) {
            return false;
        }
        return Objects.equals(this.container, other.container) &&
                this.managed == other.managed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(container, managed);
    }

    @Override
    public String toString() {
        return "ManagedSeContainer[" +
                "container=" + container + ", " +
                "managed=" + managed + ']';
    }

    @SuppressWarnings("unchecked")
    private static ManagedSeContainer initialize() {
        CDI<Object> current = null;
        try {
            current = CDI.current();
        } catch (final IllegalStateException ignored) {
        }
        if (current instanceof WeldContainer seContainer) {
            LOGGER.debug("Using existing CDI container");
            return new ManagedSeContainer(seContainer, false);
        } else if (current != null) {
            throw new IllegalStateException(
                    "Weld is the required implementation. Currently running container is not a Weld container: %s"
                            .formatted(current.getClass()));
        }
        LOGGER.debug("No CDI container found, initializing Weld");
        final Weld weld = new Weld("resteasy-vertx-cdi-container")
                .addBeanDefiningAnnotations(Path.class, Provider.class, ApplicationPath.class)
                .skipShutdownHook();
        return new ManagedSeContainer(weld.initialize(), true);
    }

}
