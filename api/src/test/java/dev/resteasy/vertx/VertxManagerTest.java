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
    public void vertxReturnsNonNull() {
        try (VertxManager manager = VertxManager.get()) {
            final Vertx vertx = manager.vertx();
            Assertions.assertNotNull(vertx);
        }
    }

    @Test
    public void vertxReturnsSameInstance() {
        final VertxManager manager = VertxManager.get();
        try (manager) {
            final Vertx first = manager.vertx();
            final Vertx second = manager.vertx();
            Assertions.assertSame(first, second);
        } finally {
            // The manager must be closed twice as two vertx() calls have happened.
            manager.close();
        }
    }

    @Test
    public void closeWithOutstandingRefKeepsVertxAlive() {
        final VertxManager manager = VertxManager.get();
        try (manager) {
            final Vertx first = manager.vertx();
            // Call vertx() again, then close it once. This should only close once the second close is invoked which
            // happens in the try-with-resources
            manager.vertx();
            manager.close();
            final Vertx stillAlive = manager.vertx();
            Assertions.assertSame(first, stillAlive, "Vertx instance should still be the same while refs remain");
        } finally {
            // The third close is required because 3 vertx() calls happen and two close invocations.
            manager.close();
        }
    }

    @Test
    public void vertxAfterFullReleaseCreatesNewInstance() {
        try (VertxManager manager = VertxManager.get()) {
            final Vertx first = manager.vertx();
            manager.close();
            final Vertx second = manager.vertx();
            Assertions.assertNotSame(first, second, "New Vertx instance should be created after full release");
        }
    }
}
