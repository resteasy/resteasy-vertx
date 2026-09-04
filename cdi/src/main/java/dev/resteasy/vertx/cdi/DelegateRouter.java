/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.cdi;

import java.util.List;
import java.util.Map;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.AllowForwardHeaders;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

/**
 * This is a simple wrapper to avoid requiring users to use {@code add-opens} directives from Vert.x to Weld. See
 * <a href="https://github.com/weld/core/issues/3502">https://github.com/weld/core/issues/3502</a> for details. If
 * Weld solves this, this wrapper can be deleted.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class DelegateRouter implements Router {
    private final Router delegate;

    DelegateRouter(final Router delegate) {
        this.delegate = delegate;
    }

    @Override
    public Router putMetadata(final String key, final Object value) {
        delegate.putMetadata(key, value);
        return this;
    }

    @Override
    public Map<String, Object> metadata() {
        return delegate.metadata();
    }

    @Override
    public <T> T getMetadata(final String key) {
        return delegate.getMetadata(key);
    }

    @Override
    public Route route() {
        return delegate.route();
    }

    @Override
    public Route route(final HttpMethod method, final String path) {
        return delegate.route(method, path);
    }

    @Override
    public Route route(final String path) {
        return delegate.route(path);
    }

    @Override
    public Route routeWithRegex(final HttpMethod method, final String regex) {
        return delegate.routeWithRegex(method, regex);
    }

    @Override
    public Route routeWithRegex(final String regex) {
        return delegate.routeWithRegex(regex);
    }

    @Override
    public Route get() {
        return delegate.get();
    }

    @Override
    public Route get(final String path) {
        return delegate.get(path);
    }

    @Override
    public Route getWithRegex(final String regex) {
        return delegate.getWithRegex(regex);
    }

    @Override
    public Route head() {
        return delegate.head();
    }

    @Override
    public Route head(final String path) {
        return delegate.head(path);
    }

    @Override
    public Route headWithRegex(final String regex) {
        return delegate.headWithRegex(regex);
    }

    @Override
    public Route options() {
        return delegate.options();
    }

    @Override
    public Route options(final String path) {
        return delegate.options(path);
    }

    @Override
    public Route optionsWithRegex(final String regex) {
        return delegate.optionsWithRegex(regex);
    }

    @Override
    public Route put() {
        return delegate.put();
    }

    @Override
    public Route put(final String path) {
        return delegate.put(path);
    }

    @Override
    public Route putWithRegex(final String regex) {
        return delegate.putWithRegex(regex);
    }

    @Override
    public Route post() {
        return delegate.post();
    }

    @Override
    public Route post(final String path) {
        return delegate.post(path);
    }

    @Override
    public Route postWithRegex(final String regex) {
        return delegate.postWithRegex(regex);
    }

    @Override
    public Route delete() {
        return delegate.delete();
    }

    @Override
    public Route delete(final String path) {
        return delegate.delete(path);
    }

    @Override
    public Route deleteWithRegex(final String regex) {
        return delegate.deleteWithRegex(regex);
    }

    @Override
    public Route trace() {
        return delegate.trace();
    }

    @Override
    public Route trace(final String path) {
        return delegate.trace(path);
    }

    @Override
    public Route traceWithRegex(final String regex) {
        return delegate.traceWithRegex(regex);
    }

    @Override
    public Route connect() {
        return delegate.connect();
    }

    @Override
    public Route connect(final String path) {
        return delegate.connect(path);
    }

    @Override
    public Route connectWithRegex(final String regex) {
        return delegate.connectWithRegex(regex);
    }

    @Override
    public Route patch() {
        return delegate.patch();
    }

    @Override
    public Route patch(final String path) {
        return delegate.patch(path);
    }

    @Override
    public Route patchWithRegex(final String regex) {
        return delegate.patchWithRegex(regex);
    }

    @Override
    public List<Route> getRoutes() {
        return delegate.getRoutes();
    }

    @Override
    public Router clear() {
        delegate.clear();
        return this;
    }

    @Override
    public Router errorHandler(final int statusCode, final Handler<RoutingContext> errorHandler) {
        delegate.errorHandler(statusCode, errorHandler);
        return this;
    }

    @Override
    public Router uncaughtErrorHandler(final Handler<RoutingContext> errorHandler) {
        delegate.uncaughtErrorHandler(errorHandler);
        return this;
    }

    @Override
    public void handleContext(final RoutingContext context) {
        delegate.handleContext(context);
    }

    @Override
    public void handleFailure(final RoutingContext context) {
        delegate.handleFailure(context);
    }

    @Override
    public Router modifiedHandler(final Handler<Router> handler) {
        delegate.modifiedHandler(handler);
        return this;
    }

    @Override
    public Router allowForward(final AllowForwardHeaders allowForwardHeaders) {
        delegate.allowForward(allowForwardHeaders);
        return this;
    }

    @Override
    public void handle(final HttpServerRequest event) {
        delegate.handle(event);
    }
}
