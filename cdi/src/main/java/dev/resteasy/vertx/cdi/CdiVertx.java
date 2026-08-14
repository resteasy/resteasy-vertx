/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.cdi;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.jboss.weld.context.bound.BoundRequestContext;

import io.vertx.core.Context;
import io.vertx.core.Deployable;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Timer;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.dns.DnsClient;
import io.vertx.core.dns.DnsClientOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpClientAgent;
import io.vertx.core.http.HttpClientBuilder;
import io.vertx.core.http.HttpClientConfig;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerBuilder;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.PoolOptions;
import io.vertx.core.http.WebSocketClient;
import io.vertx.core.http.WebSocketClientOptions;
import io.vertx.core.net.ClientSSLOptions;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.QuicClient;
import io.vertx.core.net.QuicClientConfig;
import io.vertx.core.net.QuicServer;
import io.vertx.core.net.QuicServerConfig;
import io.vertx.core.net.ServerSSLOptions;
import io.vertx.core.net.TcpClientConfig;
import io.vertx.core.net.TcpServerConfig;
import io.vertx.core.shareddata.SharedData;
import io.vertx.core.spi.VerticleFactory;

/**
 * A {@link Vertx} wrapper that propagates CDI request context to worker threads.
 * <p>
 * This wrapper intercepts {@link #executeBlocking} and {@link WorkerExecutor#executeBlocking} calls to capture the
 * CDI bean store from the current Vert.x {@link Context} at call time and re-associate it on the worker thread.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class CdiVertx implements Vertx {

    static final String CDI_BEAN_STORE_KEY = "dev.resteasy.vertx.cdi.beanStore";

    private final Vertx delegate;
    private final BoundRequestContext boundRequestContext;

    CdiVertx(final Vertx delegate, final BoundRequestContext boundRequestContext) {
        this.delegate = delegate;
        this.boundRequestContext = boundRequestContext;
    }

    @Override
    public Context getOrCreateContext() {
        return delegate.getOrCreateContext();
    }

    @Override
    public NetServer createNetServer(final TcpServerConfig config) {
        return delegate.createNetServer(config);
    }

    @Override
    public NetServer createNetServer(final TcpServerConfig config, final ServerSSLOptions sslOptions) {
        return delegate.createNetServer(config, sslOptions);
    }

    @Override
    public NetServer createNetServer(final NetServerOptions options) {
        return delegate.createNetServer(options);
    }

    @Override
    public NetServer createNetServer() {
        return delegate.createNetServer();
    }

    @Override
    public NetClient createNetClient(final TcpClientConfig config) {
        return delegate.createNetClient(config);
    }

    @Override
    public NetClient createNetClient(final TcpClientConfig config, final ClientSSLOptions sslOptions) {
        return delegate.createNetClient(config, sslOptions);
    }

    @Override
    public NetClient createNetClient(final NetClientOptions options) {
        return delegate.createNetClient(options);
    }

    @Override
    public NetClient createNetClient() {
        return delegate.createNetClient();
    }

    @Override
    public QuicServer createQuicServer(final QuicServerConfig config, final ServerSSLOptions sslOptions) {
        return delegate.createQuicServer(config, sslOptions);
    }

    @Override
    public QuicServer createQuicServer(final ServerSSLOptions sslOptions) {
        return delegate.createQuicServer(sslOptions);
    }

    @Override
    public QuicClient createQuicClient(final QuicClientConfig config, final ClientSSLOptions sslOptions) {
        return delegate.createQuicClient(config, sslOptions);
    }

    @Override
    public QuicClient createQuicClient(final ClientSSLOptions defaultSslOptions) {
        return delegate.createQuicClient(defaultSslOptions);
    }

    @Override
    public QuicClient createQuicClient(final QuicClientConfig config) {
        return delegate.createQuicClient(config);
    }

    @Override
    public HttpServer createHttpServer(final HttpServerOptions options) {
        return delegate.createHttpServer(options);
    }

    @Override
    public HttpServer createHttpServer(final HttpServerConfig config) {
        return delegate.createHttpServer(config);
    }

    @Override
    public HttpServer createHttpServer(final HttpServerConfig config, final ServerSSLOptions sslOptions) {
        return delegate.createHttpServer(config, sslOptions);
    }

    @Override
    public HttpServer createHttpServer(final ServerSSLOptions sslOptions) {
        return delegate.createHttpServer(sslOptions);
    }

    @Override
    public HttpServer createHttpServer() {
        return delegate.createHttpServer();
    }

    @Override
    public HttpServerBuilder httpServerBuilder() {
        return delegate.httpServerBuilder();
    }

    @Override
    public WebSocketClient createWebSocketClient() {
        return delegate.createWebSocketClient();
    }

    @Override
    public WebSocketClient createWebSocketClient(final WebSocketClientOptions options) {
        return delegate.createWebSocketClient(options);
    }

    @Override
    public HttpClientBuilder httpClientBuilder() {
        return delegate.httpClientBuilder();
    }

    @Override
    public HttpClientAgent createHttpClient(final HttpClientConfig clientConfig, final PoolOptions poolOptions) {
        return delegate.createHttpClient(clientConfig, poolOptions);
    }

    @Override
    public HttpClientAgent createHttpClient(final HttpClientConfig clientConfig, final ClientSSLOptions sslOptions,
            final PoolOptions poolOptions) {
        return delegate.createHttpClient(clientConfig, sslOptions, poolOptions);
    }

    @Override
    public HttpClientAgent createHttpClient(final HttpClientConfig clientConfig, final ClientSSLOptions sslOptions) {
        return delegate.createHttpClient(clientConfig, sslOptions);
    }

    @Override
    public HttpClientAgent createHttpClient(final HttpClientOptions clientOptions, final PoolOptions poolOptions) {
        return delegate.createHttpClient(clientOptions, poolOptions);
    }

    @Override
    public HttpClientAgent createHttpClient(final HttpClientConfig config) {
        return delegate.createHttpClient(config);
    }

    @Override
    public HttpClientAgent createHttpClient(final HttpClientOptions clientOptions) {
        return delegate.createHttpClient(clientOptions);
    }

    @Override
    public HttpClientAgent createHttpClient(final PoolOptions poolOptions) {
        return delegate.createHttpClient(poolOptions);
    }

    @Override
    public HttpClientAgent createHttpClient() {
        return delegate.createHttpClient();
    }

    @Override
    public DatagramSocket createDatagramSocket(final DatagramSocketOptions options) {
        return delegate.createDatagramSocket(options);
    }

    @Override
    public DatagramSocket createDatagramSocket() {
        return delegate.createDatagramSocket();
    }

    @Override
    public FileSystem fileSystem() {
        return delegate.fileSystem();
    }

    @Override
    public EventBus eventBus() {
        return delegate.eventBus();
    }

    @Override
    public DnsClient createDnsClient(final int port, final String host) {
        return delegate.createDnsClient(port, host);
    }

    @Override
    public DnsClient createDnsClient() {
        return delegate.createDnsClient();
    }

    @Override
    public DnsClient createDnsClient(final DnsClientOptions options) {
        return delegate.createDnsClient(options);
    }

    @Override
    public SharedData sharedData() {
        return delegate.sharedData();
    }

    @Override
    public Timer timer(final long delay) {
        return delegate.timer(delay);
    }

    @Override
    public Timer timer(final long delay, final TimeUnit unit) {
        return delegate.timer(delay, unit);
    }

    @Override
    public long setTimer(final long delay, final Handler<Long> handler) {
        return delegate.setTimer(delay, handler);
    }

    @Override
    public long setPeriodic(final long delay, final Handler<Long> handler) {
        return delegate.setPeriodic(delay, handler);
    }

    @Override
    public long setPeriodic(final long initialDelay, final long delay, final Handler<Long> handler) {
        return delegate.setPeriodic(initialDelay, delay, handler);
    }

    @Override
    public boolean cancelTimer(final long id) {
        return delegate.cancelTimer(id);
    }

    @Override
    public void runOnContext(final Handler<Void> action) {
        delegate.runOnContext(action);
    }

    @Override
    public Future<Void> close() {
        return delegate.close();
    }

    @Override
    public Future<String> deployVerticle(final Deployable verticle) {
        return delegate.deployVerticle(verticle);
    }

    @Override
    public Future<String> deployVerticle(final Deployable verticle, final DeploymentOptions options) {
        return delegate.deployVerticle(verticle, options);
    }

    @Override
    public Future<String> deployVerticle(final Supplier<? extends Deployable> supplier, final DeploymentOptions options) {
        return delegate.deployVerticle(supplier, options);
    }

    @Override
    public Future<String> deployVerticle(final Class<? extends Deployable> verticleClass, final DeploymentOptions options) {
        return delegate.deployVerticle(verticleClass, options);
    }

    @Override
    public Future<String> deployVerticle(final String name) {
        return delegate.deployVerticle(name);
    }

    @Override
    public Future<String> deployVerticle(final String name, final DeploymentOptions options) {
        return delegate.deployVerticle(name, options);
    }

    @Override
    public Future<Void> undeploy(final String deploymentID) {
        return delegate.undeploy(deploymentID);
    }

    @Override
    public Set<String> deploymentIDs() {
        return delegate.deploymentIDs();
    }

    @Override
    public void registerVerticleFactory(final VerticleFactory factory) {
        delegate.registerVerticleFactory(factory);
    }

    @Override
    public void unregisterVerticleFactory(final VerticleFactory factory) {
        delegate.unregisterVerticleFactory(factory);
    }

    @Override
    public Set<VerticleFactory> verticleFactories() {
        return delegate.verticleFactories();
    }

    @Override
    public boolean isClustered() {
        return delegate.isClustered();
    }

    @Override
    public <T> Future<T> executeBlocking(final Callable<T> blockingCodeHandler, final boolean ordered) {
        return delegate.executeBlocking(wrapCallable(blockingCodeHandler), ordered);
    }

    @Override
    public <T> Future<T> executeBlocking(final Callable<T> blockingCodeHandler) {
        return delegate.executeBlocking(wrapCallable(blockingCodeHandler));
    }

    @Override
    public WorkerExecutor createSharedWorkerExecutor(final String name) {
        return new CdiWorkerExecutor(delegate.createSharedWorkerExecutor(name));
    }

    @Override
    public WorkerExecutor createSharedWorkerExecutor(final String name, final int poolSize) {
        return new CdiWorkerExecutor(delegate.createSharedWorkerExecutor(name, poolSize));
    }

    @Override
    public WorkerExecutor createSharedWorkerExecutor(final String name, final int poolSize, final long maxExecuteTime) {
        return new CdiWorkerExecutor(delegate.createSharedWorkerExecutor(name, poolSize, maxExecuteTime));
    }

    @Override
    public WorkerExecutor createSharedWorkerExecutor(final String name, final int poolSize, final long maxExecuteTime,
            final TimeUnit maxExecuteTimeUnit) {
        return new CdiWorkerExecutor(delegate.createSharedWorkerExecutor(name, poolSize, maxExecuteTime, maxExecuteTimeUnit));
    }

    @Override
    public boolean isNativeTransportEnabled() {
        return delegate.isNativeTransportEnabled();
    }

    @Override
    public Throwable unavailableNativeTransportCause() {
        return delegate.unavailableNativeTransportCause();
    }

    @Override
    public Vertx exceptionHandler(final Handler<Throwable> handler) {
        delegate.exceptionHandler(handler);
        return this;
    }

    @Override
    public Handler<Throwable> exceptionHandler() {
        return delegate.exceptionHandler();
    }

    @Override
    public boolean isMetricsEnabled() {
        return delegate.isMetricsEnabled();
    }

    private <V> Callable<V> wrapCallable(final Callable<V> callable) {
        if (boundRequestContext == null) {
            return callable;
        }
        final Context vertxContext = Vertx.currentContext();
        if (vertxContext == null) {
            return callable;
        }
        final Map<String, Object> beanStore = vertxContext.get(CDI_BEAN_STORE_KEY);
        if (beanStore == null) {
            return callable;
        }
        return () -> {
            boundRequestContext.associate(beanStore);
            boundRequestContext.activate();
            try {
                return callable.call();
            } finally {
                boundRequestContext.deactivate();
                boundRequestContext.dissociate(beanStore);
            }
        };
    }

    private class CdiWorkerExecutor implements WorkerExecutor {
        private final WorkerExecutor delegate;

        private CdiWorkerExecutor(final WorkerExecutor delegate) {
            this.delegate = delegate;
        }

        @Override
        public <T> Future<T> executeBlocking(final Callable<T> blockingCodeHandler, final boolean ordered) {
            return delegate.executeBlocking(wrapCallable(blockingCodeHandler), ordered);
        }

        @Override
        public <T> Future<T> executeBlocking(final Callable<T> blockingCodeHandler) {
            return delegate.executeBlocking(wrapCallable(blockingCodeHandler));
        }

        @Override
        public boolean isMetricsEnabled() {
            return delegate.isMetricsEnabled();
        }

        @Override
        public Future<Void> close() {
            return delegate.close();
        }
    }
}
