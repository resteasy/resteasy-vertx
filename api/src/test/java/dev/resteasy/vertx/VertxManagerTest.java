/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;

public class VertxManagerTest {

    @Test
    public void acquireReturnsNonNull() {
        try (VertxManager manager = VertxManager.instance()) {
            final Vertx vertx = manager.acquire();
            Assertions.assertNotNull(vertx);
        }
    }

    @Test
    public void acquireReturnsSameInstance() {
        final VertxManager manager = VertxManager.instance();
        try (manager) {
            final Vertx first = manager.acquire();
            final Vertx second = manager.acquire();
            Assertions.assertSame(first, second);
        } finally {
            // The manager must be closed twice as two acquires have happened.
            manager.close();
        }
    }

    @Test
    public void closeWithOutstandingRefKeepsVertxAlive() {
        final VertxManager manager = VertxManager.instance();
        try (manager) {
            final Vertx first = manager.acquire();
            // Acquire an again, then close it once. This should only close once the second close is invoked which
            // happens in the try-with-resources
            manager.acquire();
            manager.close();
            final Vertx stillAlive = manager.acquire();
            Assertions.assertSame(first, stillAlive, "Vertx instance should still be the same while refs remain");
        } finally {
            // The third close is required because 3 acquires happen and two close invocation.
            manager.close();
        }
    }

    @Test
    public void acquireAfterFullReleaseCreatesNewInstance() {
        try (VertxManager manager = VertxManager.instance()) {
            final Vertx first = manager.acquire();
            manager.close();
            final Vertx second = manager.acquire();
            Assertions.assertNotSame(first, second, "New Vertx instance should be created after full release");
        }
    }
}
