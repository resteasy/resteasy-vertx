/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.client.jaxrs.engines.vertx;

import java.util.concurrent.TimeUnit;

import org.jboss.resteasy.client.jaxrs.api.ClientBuilderConfiguration;
import org.jboss.resteasy.client.jaxrs.engine.ClientHttpEngineFactory;
import org.jboss.resteasy.client.jaxrs.engines.AsyncClientHttpEngine;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class VertxClientHttpEngineFactory implements ClientHttpEngineFactory {
    @Override
    public AsyncClientHttpEngine asyncHttpClientEngine(final ClientBuilderConfiguration configuration) {
        final Vertx vertx = Vertx.vertx();

        final HttpClientOptions options = new HttpClientOptions();

        final long connectionTimeout = configuration.connectionTimeout(TimeUnit.MILLISECONDS);
        if (connectionTimeout > 0L) {
            options.setConnectTimeout(Math.toIntExact(connectionTimeout));
        }

        final long idleTimeout = configuration.connectionIdleTime(TimeUnit.SECONDS);
        if (idleTimeout > 0L) {
            options.setIdleTimeout(Math.toIntExact(idleTimeout));
        }

        final String proxyHostname = configuration.defaultProxyHostname();
        if (proxyHostname != null) {
            final ProxyOptions proxyOptions = new ProxyOptions();
            proxyOptions.setHost(proxyHostname);
            proxyOptions.setPort(configuration.defaultProxyPort());
            proxyOptions.setType(ProxyType.HTTP);
            options.setProxyOptions(proxyOptions);
        }

        final long readTimeout = configuration.readTimeout(TimeUnit.SECONDS);
        if (readTimeout > 0L) {
            options.setReadIdleTimeout(Math.toIntExact(readTimeout));
        }

        return new VertxClientHttpEngine(vertx, options, configuration);
    }
}
