/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.client.ResponseProcessingException;
import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.api.ClientBuilderConfiguration;
import org.jboss.resteasy.client.jaxrs.engines.AsyncClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.internal.ClientConfiguration;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.resteasy.client.jaxrs.internal.FinalizedClientResponse;
import org.jboss.resteasy.concurrent.ContextualExecutors;
import org.jboss.resteasy.tracing.RESTEasyTracingLogger;
import org.jboss.resteasy.util.CaseInsensitiveMap;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;

import dev.resteasy.vertx.VertxManager;

/**
 * A RESTEasy HTTP client engine implementation backed by Vert.x HTTP client.
 * <p>
 * This engine provides asynchronous HTTP request execution using Vert.x's event-driven architecture.
 * Request bodies are streamed using chunked transfer encoding when no explicit Content-Length is provided,
 * preventing excessive memory consumption for large payloads.
 * </p>
 * <p>
 * The engine can be configured with a custom {@link Vertx} instance, {@link HttpClientOptions},
 * and {@link ClientBuilderConfiguration}. If no Vertx instance is provided, a default one will be created
 * and managed by this engine.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class VertxClientHttpEngine implements AsyncClientHttpEngine {

    private static final Logger LOGGER = Logger.getLogger(VertxClientHttpEngine.class);

    /**
     * Default buffer size for response body streams (4KB).
     * This balances memory usage with throughput for typical HTTP responses.
     */
    private static final int DEFAULT_RESPONSE_BUFFER_SIZE = 4 * 1024;

    private final HttpClient httpClient;
    private final ClientBuilderConfiguration configuration;

    /**
     * Creates a new engine.
     */
    public VertxClientHttpEngine() {
        this.httpClient = createClient(null);
        this.configuration = null;
    }

    /**
     * Creates a new engine with the specified HTTP client options.
     *
     * @param options the HTTP client options
     */
    public VertxClientHttpEngine(final HttpClientOptions options) {
        this.httpClient = createClient(options);
        this.configuration = null;
    }

    /**
     * Creates a new engine with the specified HTTP client options and configuration.
     *
     * @param options       the HTTP client options
     * @param configuration the client builder configuration, may be {@code null}
     */
    public VertxClientHttpEngine(final HttpClientOptions options, final ClientBuilderConfiguration configuration) {
        this.httpClient = createClient(options);
        this.configuration = configuration;
    }

    @Override
    public <T> Future<T> submit(final ClientInvocation request,
            final boolean buffered,
            final InvocationCallback<T> callback,
            final ResultExtractor<T> extractor) {
        CompletableFuture<T> future = submit(request).thenCompose(response -> {
            CompletableFuture<T> tmp = new CompletableFuture<>();
            final ExecutorService executor = resolveExecutor(null);
            executor.execute(() -> {
                try {
                    T result = extractor.extractResult(response);
                    tmp.complete(result);
                } catch (Exception e) {
                    tmp.completeExceptionally(e);
                }
            });
            return tmp;
        });
        if (callback != null) {
            future = future.whenComplete((response, throwable) -> {
                if (throwable != null) {
                    callback.failed(throwable);
                } else {
                    callback.completed(response);
                }
            });
        }
        return future;
    }

    @Override
    public <T> CompletableFuture<T> submit(final ClientInvocation request,
            final boolean buffered,
            final ResultExtractor<T> extractor,
            final ExecutorService executorService) {
        return submit(request).thenCompose(response -> {
            final CompletableFuture<T> tmp = new CompletableFuture<>();
            final ExecutorService executor = resolveExecutor(executorService);
            executor.execute(() -> {
                try {
                    T result = extractor.extractResult(response);
                    tmp.complete(result);
                } catch (Exception e) {
                    tmp.completeExceptionally(e);
                }
            });
            return tmp;
        });
    }

    private CompletableFuture<ClientResponse> submit(final ClientInvocation request) {
        final HttpMethod method = HttpMethod.valueOf(request.getMethod());

        final RequestOptions options = new RequestOptions();
        options.setMethod(method);
        if (configuration != null) {
            options.setFollowRedirects(configuration.isFollowRedirects());
        }
        final MultiMap headers = MultiMap.caseInsensitiveMultiMap();
        request.getHeaders().asMap().forEach(headers::add);

        // If an entity is present but no content length was explicitly provided,
        // we must use chunked transfer encoding since we are streaming and don't know the size.
        if (request.getEntity() != null) {
            if (!headers.contains(HttpHeaders.CONTENT_LENGTH)) {
                headers.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
            }
        }

        options.setHeaders(headers);

        if (!headers.contains(HttpHeaders.USER_AGENT)) {
            options.addHeader(HttpHeaders.USER_AGENT.toString(), "Vertx");
        }

        final URI uri = request.getUri();
        options.setHost(uri.getHost());

        if (uri.getPort() < 0) {
            if ("http".equals(uri.getScheme())) {
                options.setPort(80);
            } else if ("https".equals(uri.getScheme())) {
                options.setPort(443);
            }
        } else {
            options.setPort(uri.getPort());
        }

        String relativeUri = uri.getRawPath();
        if (uri.getRawQuery() != null && !uri.getRawQuery().trim().isEmpty()) {
            relativeUri = relativeUri + "?" + uri.getRawQuery();
        }
        options.setURI(relativeUri);

        if (request.getConfiguration().hasProperty(VertxClientProperties.REQUEST_TIMEOUT)) {
            long timeoutMs = unwrapTimeout(
                    request.getConfiguration().getProperty(VertxClientProperties.REQUEST_TIMEOUT));
            if (timeoutMs > 0) {
                options.setTimeout(timeoutMs);
            }
        }

        final CompletableFuture<ClientResponse> futureResponse = new CompletableFuture<>();

        httpClient.request(options).onComplete(ar -> {
            if (ar.failed()) {
                futureResponse.completeExceptionally(ar.cause());
                return;
            }

            final HttpClientRequest clientRequest = ar.result();

            // 1. Setup the Response Handler
            clientRequest.response().onComplete(res -> {
                if (res.succeeded()) {
                    HttpClientResponse response = res.result();
                    response.pause();
                    futureResponse.complete(toClientResponse(request.getClientConfiguration(), response));
                    response.resume();
                } else {
                    futureResponse.completeExceptionally(res.cause());
                }
            });

            // 2. Handle the Request Body (if present)
            if (request.getEntity() != null) {
                final ExecutorService executor = resolveExecutor(null);
                final io.vertx.core.Context context = Vertx.currentContext();

                // Push the blocking JAX-RS writer to a worker thread
                executor.execute(() -> {
                    final VertxChunkedOutputStream vos = new VertxChunkedOutputStream(clientRequest, context);
                    // Do not use try-with-resources here as this is an extended try-with-resources block. We need to
                    // invoke the reset(0) before the stream itself is closed.
                    //noinspection TryFinallyCanBeTryWithResources
                    try {
                        request.getDelegatingOutputStream().setDelegate(vos);
                        request.writeRequestBody(request.getEntityStream());
                    } catch (Throwable t) {
                        clientRequest.reset(0);
                        if (!futureResponse.completeExceptionally(t)) {
                            LOGGER.debug("Request body write failed after response was already received", t);
                        }
                    } finally {
                        try {
                            vos.close();
                        } catch (IOException e) {
                            LOGGER.trace("Error closing output stream", e);
                        }
                    }
                });
            } else {
                // No body, just end the request
                clientRequest.end();
            }
        });

        return futureResponse;
    }

    private long unwrapTimeout(final Object timeout) {
        if (timeout instanceof Duration) {
            return ((Duration) timeout).toMillis();
        } else if (timeout instanceof Number) {
            return ((Number) timeout).longValue();
        } else if (timeout != null) {
            return Long.parseLong(timeout.toString());
        } else {
            return -1L;
        }
    }

    @Override
    public SSLContext getSslContext() {
        // Vertx does not allow to access the ssl-context from HttpClient API.
        throw new UnsupportedOperationException();
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        // Vertx does not support HostnameVerifier API.
        throw new UnsupportedOperationException();
    }

    @Override
    public Response invoke(Invocation request) {
        final Future<ClientResponse> future = submit((ClientInvocation) request);

        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw clientException(e, null);
        } catch (ExecutionException e) {
            throw clientException(e.getCause(), null);
        }
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } finally {
            VertxManager.get().close();
        }
    }

    private ExecutorService resolveExecutor(final ExecutorService executorService) {
        if (configuration == null) {
            return executorService == null ? ContextualExecutors.threadPool() : executorService;
        }
        return executorService == null ? configuration.executorService().orElse(ContextualExecutors.threadPool())
                : ContextualExecutors.wrap(executorService);
    }

    static RuntimeException clientException(final Throwable ex, final Response clientResponse) {
        RuntimeException ret;
        if (ex == null) {
            ret = new ProcessingException(new NullPointerException());
        } else if (ex instanceof WebApplicationException) {
            ret = (WebApplicationException) ex;
        } else if (ex instanceof ProcessingException) {
            ret = (ProcessingException) ex;
        } else if (clientResponse != null) {
            ret = new ResponseProcessingException(clientResponse, ex);
        } else {
            ret = new ProcessingException(ex);
        }
        return ret;
    }

    private ClientResponse toClientResponse(final ClientConfiguration clientConfiguration,
            final HttpClientResponse clientResponse) {

        VertxInputStream adapter = new VertxInputStream(clientResponse, DEFAULT_RESPONSE_BUFFER_SIZE);

        class RestEasyClientResponse extends FinalizedClientResponse {

            private InputStream is;

            private RestEasyClientResponse(final ClientConfiguration configuration) {
                super(configuration, RESTEasyTracingLogger.empty());
                this.is = adapter;
            }

            @Override
            protected InputStream getInputStream() {
                return this.is;
            }

            @Override
            protected void setInputStream(InputStream inputStream) {
                this.is = inputStream;
            }

            @Override
            public void releaseConnection() throws IOException {
                this.releaseConnection(false);
            }

            @Override
            public void releaseConnection(boolean consumeInputStream) throws IOException {
                try {
                    if (is != null) {
                        if (consumeInputStream) {
                            while (is.read() != -1) {
                                // drain
                            }
                        }
                        is.close();
                    }
                } catch (IOException e) {
                    // Swallowing because other ClientHttpEngine implementations are swallowing as well.
                    // What is better?  causing a potential leak with inputstream slowly or cause an unexpected
                    // and unhandled io error and potentially cause the service go down?
                    // log.warn("Exception while releasing the connection!", e);
                }
            }
        }
        ClientResponse restEasyClientResponse = new RestEasyClientResponse(clientConfiguration);
        restEasyClientResponse.setStatus(clientResponse.statusCode());
        CaseInsensitiveMap<String> restEasyHeaders = new CaseInsensitiveMap<>();
        clientResponse.headers().forEach(header -> restEasyHeaders.add(header.getKey(), header.getValue()));
        restEasyClientResponse.setHeaders(restEasyHeaders);
        return restEasyClientResponse;
    }

    private static HttpClient createClient(final HttpClientOptions options) {
        final Vertx vertx = VertxManager.get().vertx();
        try {
            final HttpClientOptions httpOptions = options == null ? new HttpClientOptions() : options;
            return vertx.createHttpClient(httpOptions);
        } catch (Throwable t) {
            VertxManager.get().close();
            throw t;
        }
    }
}
