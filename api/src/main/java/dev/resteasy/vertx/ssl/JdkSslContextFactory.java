/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.ssl;

import java.util.List;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;

import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import io.netty.handler.ssl.SslContext;
import io.vertx.core.spi.tls.SslContextFactory;

/**
 * A {@link SslContextFactory} that wraps an existing {@link SSLContext} into a Netty {@link JdkSslContext}.
 * <p>
 * The key and trust material is fully encapsulated by the provided {@link SSLContext}; calls to
 * {@link #keyMananagerFactory(KeyManagerFactory)} and {@link #trustManagerFactory(TrustManagerFactory)}
 * are ignored.
 * </p>
 */
class JdkSslContextFactory implements SslContextFactory {

    private final SSLContext sslContext;
    private boolean isClient;
    private ClientAuth clientAuth = ClientAuth.NONE;
    private Set<String> enabledCipherSuites;
    private Set<String> enabledProtocols;
    private List<String> applicationProtocols;

    JdkSslContextFactory(final SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    @Override
    public SslContextFactory forServer(final ClientAuth clientAuth) {
        this.isClient = false;
        this.clientAuth = clientAuth;
        return this;
    }

    @Override
    public SslContextFactory forClient(final SNIServerName serverName, final String endpointIdentificationAlgorithm) {
        this.isClient = true;
        return this;
    }

    @Override
    public SslContextFactory enabledCipherSuites(final Set<String> enabledCipherSuites) {
        this.enabledCipherSuites = enabledCipherSuites;
        return this;
    }

    @Override
    public SslContextFactory enabledProtocols(final Set<String> enabledProtocols) {
        this.enabledProtocols = enabledProtocols;
        return this;
    }

    @Override
    public SslContextFactory applicationProtocols(final List<String> applicationProtocols) {
        this.applicationProtocols = applicationProtocols;
        return this;
    }

    @Override
    public SslContext create() throws SSLException {
        ApplicationProtocolConfig apn = null;
        if (applicationProtocols != null && !applicationProtocols.isEmpty()) {
            apn = new ApplicationProtocolConfig(
                    ApplicationProtocolConfig.Protocol.ALPN,
                    ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                    ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                    applicationProtocols);
        }
        final Iterable<String> ciphers = enabledCipherSuites != null && !enabledCipherSuites.isEmpty()
                ? enabledCipherSuites
                : null;
        final String[] protocols = enabledProtocols != null && !enabledProtocols.isEmpty()
                ? enabledProtocols.toArray(String[]::new)
                : null;
        return new JdkSslContext(sslContext, isClient, ciphers, IdentityCipherSuiteFilter.INSTANCE, apn,
                isClient ? ClientAuth.NONE : clientAuth, protocols, false);
    }
}
