/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.client;

import java.time.Duration;

import io.vertx.core.http.HttpClientOptions;

/**
 * Property names for configuring the Vert.x client engine via
 * {@link jakarta.ws.rs.client.ClientBuilder#property(String, Object)}.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public final class VertxClientProperties {

    /**
     * Client configuration property for a per-request timeout. The value can be a {@link Duration}, {@link Number}
     * (milliseconds), or a string parseable as a long.
     */
    public static final String REQUEST_TIMEOUT = "dev.resteasy.vertx.client.request.timeout";

    /**
     * Client configuration property for custom {@link HttpClientOptions}. When set, the provided options are used
     * as-is for the Vert.x HTTP client. Standard {@link jakarta.ws.rs.client.ClientBuilder} settings (timeouts,
     * SSL context, proxy) are <em>not</em> applied — all configuration must be set directly on the provided options.
     */
    public static final String HTTP_CLIENT_OPTIONS = "dev.resteasy.vertx.client.http.options";

    private VertxClientProperties() {
    }
}
