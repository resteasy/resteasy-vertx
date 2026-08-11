/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.config;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.jboss.resteasy.spi.config.Options;

/**
 * Configuration options for RESTEasy Vert.x integration.
 * <p>
 * Options can be set via system properties or other configuration sources supported by
 * {@link Options}. For example:
 * </p>
 *
 * <pre>
 * -Ddev.resteasy.vertx.timeout=60
 * -Ddev.resteasy.vertx.timeout.unit=SECONDS
 * </pre>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class ResteasyVertxOptions<T> extends Options<T> {

    /**
     * The timeout value for blocking operations such as shutting down the {@link io.vertx.core.Vertx} instance.
     * <p>
     * System property: {@code dev.resteasy.vertx.timeout}<br>
     * Default: {@code 30}
     * </p>
     */
    public static final ResteasyVertxOptions<Long> TIMEOUT = new ResteasyVertxOptions<>("dev.resteasy.vertx.timeout",
            Long.class,
            () -> 30L);

    /**
     * The time unit for {@link #TIMEOUT}.
     * <p>
     * System property: {@code dev.resteasy.vertx.timeout.unit}<br>
     * Default: {@link TimeUnit#SECONDS}
     * </p>
     */
    public static final ResteasyVertxOptions<TimeUnit> TIMEOUT_UNIT = new ResteasyVertxOptions<>(
            "dev.resteasy.vertx.timeout.unit",
            TimeUnit.class, () -> TimeUnit.SECONDS);

    /**
     * The maximum allowed request body size in bytes for the embedded server.
     * Requests exceeding this limit will be rejected with HTTP 413 (Payload Too Large).
     * A value of {@code -1} disables the limit.
     * <p>
     * System property: {@code dev.resteasy.vertx.server.max.request.size}<br>
     * Default: {@code 10485760} (10 MB)
     * </p>
     */
    public static final ResteasyVertxOptions<Long> MAX_REQUEST_SIZE = new ResteasyVertxOptions<>(
            "dev.resteasy.vertx.server.max.request.size",
            Long.class, () -> 10 * 1024 * 1024L);

    private ResteasyVertxOptions(final String key, final Class<T> name, final Supplier<T> dftValue) {
        super(key, name, dftValue);
    }
}
