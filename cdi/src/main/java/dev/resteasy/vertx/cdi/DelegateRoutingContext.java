/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.cdi;

import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.LanguageHeader;
import io.vertx.ext.web.ParsedHeaderValues;
import io.vertx.ext.web.RequestBody;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.UserContext;

/**
 * This is a simple wrapper to avoid requiring users to use {@code add-opens} directives from Vert.x to Weld. See
 * <a href="https://github.com/weld/core/issues/3502">https://github.com/weld/core/issues/3502</a> for details. If
 * Weld solves this, this wrapper can be deleted.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class DelegateRoutingContext implements RoutingContext {
    private final RoutingContext delegate;

    DelegateRoutingContext(final RoutingContext delegate) {
        this.delegate = delegate;
    }

    @Override
    public HttpServerRequest request() {
        return delegate.request();
    }

    @Override
    public HttpServerResponse response() {
        return delegate.response();
    }

    @Override
    public void next() {
        delegate.next();
    }

    @Override
    public void fail(final int statusCode) {
        delegate.fail(statusCode);
    }

    @Override
    public void fail(final Throwable throwable) {
        delegate.fail(throwable);
    }

    @Override
    public void fail(final int statusCode, final Throwable throwable) {
        delegate.fail(statusCode, throwable);
    }

    @Override
    public RoutingContext put(final String key, final Object obj) {
        delegate.put(key, obj);
        return this;
    }

    @Override
    public <T> T get(final String key) {
        return delegate.get(key);
    }

    @Override
    public <T> T get(final String key, final T defaultValue) {
        return delegate.get(key, defaultValue);
    }

    @Override
    public <T> T remove(final String key) {
        return delegate.remove(key);
    }

    @Override
    public <T> Map<String, T> data() {
        return delegate.data();
    }

    @Override
    public Vertx vertx() {
        return delegate.vertx();
    }

    @Override
    public String mountPoint() {
        return delegate.mountPoint();
    }

    @Override
    public Route currentRoute() {
        return delegate.currentRoute();
    }

    @Override
    public String normalizedPath() {
        return delegate.normalizedPath();
    }

    @Override
    public RequestBody body() {
        return delegate.body();
    }

    @Override
    public List<FileUpload> fileUploads() {
        return delegate.fileUploads();
    }

    @Override
    public void cancelAndCleanupFileUploads() {
        delegate.cancelAndCleanupFileUploads();
    }

    @Override
    public Session session() {
        return delegate.session();
    }

    @Override
    public boolean isSessionAccessed() {
        return delegate.isSessionAccessed();
    }

    @Override
    public UserContext userContext() {
        return delegate.userContext();
    }

    @Override
    public User user() {
        return delegate.user();
    }

    @Override
    public Throwable failure() {
        return delegate.failure();
    }

    @Override
    public int statusCode() {
        return delegate.statusCode();
    }

    @Override
    public String getAcceptableContentType() {
        return delegate.getAcceptableContentType();
    }

    @Override
    public ParsedHeaderValues parsedHeaders() {
        return delegate.parsedHeaders();
    }

    @Override
    public int addHeadersEndHandler(final Handler<Void> handler) {
        return delegate.addHeadersEndHandler(handler);
    }

    @Override
    public boolean removeHeadersEndHandler(final int handlerID) {
        return delegate.removeHeadersEndHandler(handlerID);
    }

    @Override
    public int addBodyEndHandler(final Handler<Void> handler) {
        return delegate.addBodyEndHandler(handler);
    }

    @Override
    public boolean removeBodyEndHandler(final int handlerID) {
        return delegate.removeBodyEndHandler(handlerID);
    }

    @Override
    public int addEndHandler(final Handler<AsyncResult<Void>> handler) {
        return delegate.addEndHandler(handler);
    }

    @Override
    public boolean removeEndHandler(final int handlerID) {
        return delegate.removeEndHandler(handlerID);
    }

    @Override
    public boolean failed() {
        return delegate.failed();
    }

    @Override
    public void setAcceptableContentType(final String contentType) {
        delegate.setAcceptableContentType(contentType);
    }

    @Override
    public void reroute(final String path) {
        delegate.reroute(path);
    }

    @Override
    public void reroute(final HttpMethod method, final String path) {
        delegate.reroute(method, path);
    }

    @Override
    public List<LanguageHeader> acceptableLanguages() {
        return delegate.acceptableLanguages();
    }

    @Override
    public LanguageHeader preferredLanguage() {
        return delegate.preferredLanguage();
    }

    @Override
    public Map<String, String> pathParams() {
        return delegate.pathParams();
    }

    @Override
    public String pathParam(final String name) {
        return delegate.pathParam(name);
    }

    @Override
    public MultiMap queryParams() {
        return delegate.queryParams();
    }

    @Override
    public MultiMap queryParams(final Charset encoding) {
        return delegate.queryParams(encoding);
    }

    @Override
    public List<String> queryParam(final String name) {
        return delegate.queryParam(name);
    }

    @Override
    public RoutingContext attachment(final String filename) {
        delegate.attachment(filename);
        return this;
    }

    @Override
    public Future<Void> redirect(final String url) {
        return delegate.redirect(url);
    }

    @Override
    public Future<Void> json(final Object json) {
        return delegate.json(json);
    }

    @Override
    public boolean is(final String type) {
        return delegate.is(type);
    }

    @Override
    public boolean isFresh() {
        return delegate.isFresh();
    }

    @Override
    public RoutingContext etag(final String etag) {
        delegate.etag(etag);
        return this;
    }

    @Override
    public RoutingContext lastModified(final Instant instant) {
        delegate.lastModified(instant);
        return this;
    }

    @Override
    public RoutingContext lastModified(final String instant) {
        delegate.lastModified(instant);
        return this;
    }

    @Override
    public Future<Void> end(final String chunk) {
        return delegate.end(chunk);
    }

    @Override
    public Future<Void> end(final Buffer buffer) {
        return delegate.end(buffer);
    }

    @Override
    public Future<Void> end() {
        return delegate.end();
    }
}
