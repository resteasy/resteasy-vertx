/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.server.vertx;

import java.util.Locale;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.SeBootstrap.Configuration;
import jakarta.ws.rs.SeBootstrap.Configuration.SSLClientAuthentication;

import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.plugins.server.embedded.EmbeddedServer;
import org.jboss.resteasy.plugins.server.embedded.EmbeddedServers;
import org.jboss.resteasy.spi.ResteasyDeployment;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;

import dev.resteasy.server.vertx._private.VertxLogger;
import dev.resteasy.vertx.VertxManager;
import dev.resteasy.vertx.api.RouterFactory;
import dev.resteasy.vertx.config.ResteasyVertxOptions;
import dev.resteasy.vertx.ssl.SslContextConverter;

/**
 * Embedded server implementation using Vert.x HTTP server with Vert.x Web {@link Router}.
 * <p>
 * This server integrates RESTEasy with Vert.x's async, event-driven HTTP server. It implements
 * the {@link EmbeddedServer} interface for SeBootstrap integration.
 * </p>
 * <p>
 * The server uses a {@link Router} for request handling, which enables Vert.x Web middleware
 * (CORS, sessions, authentication, etc.) to be added via a custom {@link RouterFactory}.
 * </p>
 * <p>
 * The server can be configured programmatically via SeBootstrap:
 * </p>
 *
 * <pre>{@code
 * SeBootstrap.start(MyApplication.class, config)
 *         .thenAccept(instance -> {
 *             // Server running
 *         });
 * }</pre>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @since 2.0
 */
public class VertxEmbeddedServer implements EmbeddedServer {

    private final Lock lock = new ReentrantLock();
    private final ResteasyDeployment deployment;

    private HttpServer httpServer;

    /**
     * No-arg constructor required by ServiceLoader.
     */
    public VertxEmbeddedServer() {
        this.deployment = new ResteasyDeploymentImpl();
    }

    @Override
    public void start(final Configuration configuration) {
        lock.lock();
        try {
            if (httpServer != null) {
                throw VertxLogger.LOGGER.serverAlreadyStarted();
            }
            EmbeddedServers.validateDeployment(deployment);
            final Vertx vertx = VertxManager.instance().acquire();
            try {
                final HttpServerOptions serverOptions = new HttpServerOptions();

                // Configure SSL if HTTPS
                if ("HTTPS".equalsIgnoreCase(configuration.protocol())) {
                    SSLContext sslContext = configuration.sslContext();
                    if (sslContext == null) {
                        sslContext = SSLContext.getDefault();
                    }
                    SslContextConverter.configureSsl(serverOptions, sslContext,
                            toClientAuth(configuration.sslClientAuthentication()));
                }

                // Determine the context path
                final String applicationContextPath = EmbeddedServers.checkContextPath(deployment);
                final String configContextPath = configuration.rootPath();
                final String contextPath;
                if (configContextPath.equals(applicationContextPath)) {
                    contextPath = applicationContextPath;
                } else if (configContextPath.equals("/")) {
                    contextPath = applicationContextPath;
                } else {
                    contextPath = configContextPath;
                }

                // Create router via factory
                final Router router = createRouter(vertx, configuration);

                // Install the RESTEasy handler
                final VertxRoutingContextHandler handler = new VertxRoutingContextHandler(vertx, router, deployment,
                        contextPath);
                router.route().handler(handler);

                // Create and start HTTP server
                httpServer = vertx.createHttpServer(serverOptions);

                Future<HttpServer> listenFuture = httpServer
                        .requestHandler(router)
                        .listen(configuration.port(), configuration.host());
                listenFuture.mapEmpty()
                        .toCompletionStage()
                        .toCompletableFuture()
                        .join();
            } catch (Exception e) {
                if (httpServer != null) {
                    try {
                        httpServer.close();
                    } catch (Exception e1) {
                        VertxLogger.LOGGER.trace("Failed to close http server", e1);
                    }
                    httpServer = null;
                }
                VertxManager.instance().close();
                throw VertxLogger.LOGGER.failedToStartServer(e);
            }
        } finally {
            lock.unlock();
        }

    }

    @Override
    public void stop() {
        lock.lock();
        try {
            try {
                deployment.stop();
            } catch (Exception e) {
                VertxLogger.LOGGER.debugf(e, "Failed to stop deployment %s", deployment);
            }
            if (httpServer != null) {
                final long timeout = ResteasyVertxOptions.TIMEOUT.getValue();
                final TimeUnit unit = ResteasyVertxOptions.TIMEOUT_UNIT.getValue();
                final HttpServer httpServer = this.httpServer;
                this.httpServer = null;
                try {
                    httpServer.close().await(timeout, unit);
                } catch (TimeoutException e) {
                    throw VertxLogger.LOGGER.failedToShutdownServer(e, timeout, unit.name().toLowerCase(Locale.ROOT));
                } catch (Exception e) {
                    // There is a Utils.throwAsUnchecked(e), so we need to check for an InterruptedException
                    //noinspection ConstantValue
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    throw VertxLogger.LOGGER.failedToShutdownServer(e, timeout, unit.name().toLowerCase(Locale.ROOT));
                } finally {
                    VertxManager.instance().close();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ResteasyDeployment getDeployment() {
        return deployment;
    }

    private static ClientAuth toClientAuth(final SSLClientAuthentication sslClientAuthentication) {
        if (sslClientAuthentication == null) {
            return ClientAuth.NONE;
        }
        return switch (sslClientAuthentication) {
            case MANDATORY -> ClientAuth.REQUIRED;
            case OPTIONAL -> ClientAuth.REQUEST;
            default -> ClientAuth.NONE;
        };
    }

    private static Router createRouter(final Vertx vertx, final Configuration configuration) {
        RouterFactory factory;
        if (configuration.hasProperty(RouterFactory.PROPERTY)) {
            factory = (RouterFactory) configuration.property(RouterFactory.PROPERTY);
        } else {
            factory = ServiceLoader.load(RouterFactory.class)
                    .findFirst()
                    .orElse(Router::router);
        }
        return factory.create(vertx);
    }
}
