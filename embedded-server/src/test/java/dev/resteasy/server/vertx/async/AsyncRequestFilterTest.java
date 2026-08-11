/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.server.vertx.async;

import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

@RestBootstrap({
        AsyncRequestFilterResource.class,
        AsyncRequestFilter1.class, AsyncRequestFilter2.class, AsyncRequestFilter3.class,
        AsyncPreMatchRequestFilter1.class, AsyncPreMatchRequestFilter2.class, AsyncPreMatchRequestFilter3.class,
        AsyncResponseFilter1.class, AsyncResponseFilter2.class, AsyncResponseFilter3.class,
        AsyncFilterException.class, AsyncFilterExceptionMapper.class
})
public class AsyncRequestFilterTest {

    @RestResource
    private WebTarget base;

    /**
     * @tpTestDetails Interceptors work
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testRequestFilters() throws Exception {
        // all sync

        Response response = base.request()
                .header("Filter1", "sync-pass")
                .header("Filter2", "sync-pass")
                .header("Filter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        response = base.request()
                .header("Filter1", "sync-fail")
                .header("Filter2", "sync-fail")
                .header("Filter3", "sync-fail")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("Filter1", response.readEntity(String.class));

        response = base.request()
                .header("Filter1", "sync-pass")
                .header("Filter2", "sync-fail")
                .header("Filter3", "sync-fail")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("Filter2", response.readEntity(String.class));

        response = base.request()
                .header("Filter1", "sync-pass")
                .header("Filter2", "sync-pass")
                .header("Filter3", "sync-fail")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("Filter3", response.readEntity(String.class));

        // async
        response = base.request()
                .header("Filter1", "async-pass")
                .header("Filter2", "sync-pass")
                .header("Filter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        response = base.request()
                .header("Filter1", "async-pass")
                .header("Filter2", "async-pass")
                .header("Filter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        response = base.request()
                .header("Filter1", "async-pass")
                .header("Filter2", "async-pass")
                .header("Filter3", "async-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        response = base.request()
                .header("Filter1", "async-pass")
                .header("Filter2", "sync-pass")
                .header("Filter3", "async-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        response = base.request()
                .header("Filter1", "sync-pass")
                .header("Filter2", "async-pass")
                .header("Filter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        // async failures

        response = base.request()
                .header("Filter1", "async-fail")
                .header("Filter2", "sync-fail")
                .header("Filter3", "sync-fail")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("Filter1", response.readEntity(String.class));

        response = base.request()
                .header("Filter1", "async-pass")
                .header("Filter2", "sync-fail")
                .header("Filter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("Filter2", response.readEntity(String.class));

        response = base.request()
                .header("Filter1", "async-pass")
                .header("Filter2", "async-fail")
                .header("Filter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("Filter2", response.readEntity(String.class));

        // async instantaneous
        response = base.request()
                .header("Filter1", "async-pass-instant")
                .header("Filter2", "sync-pass")
                .header("Filter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        response = base.request()
                .header("Filter1", "async-fail-instant")
                .header("Filter2", "sync-pass")
                .header("Filter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("Filter1", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Interceptors work
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testPreMatchRequestFilters() throws Exception {

        // all sync

        Response response = base.request()
                .header("PreMatchFilter1", "sync-pass")
                .header("PreMatchFilter2", "sync-pass")
                .header("PreMatchFilter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        response = base.request()
                .header("PreMatchFilter1", "sync-fail")
                .header("PreMatchFilter2", "sync-fail")
                .header("PreMatchFilter3", "sync-fail")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("PreMatchFilter1", response.readEntity(String.class));

        response = base.request()
                .header("PreMatchFilter1", "sync-pass")
                .header("PreMatchFilter2", "sync-fail")
                .header("PreMatchFilter3", "sync-fail")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("PreMatchFilter2", response.readEntity(String.class));

        response = base.request()
                .header("PreMatchFilter1", "sync-pass")
                .header("PreMatchFilter2", "sync-pass")
                .header("PreMatchFilter3", "sync-fail")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("PreMatchFilter3", response.readEntity(String.class));

        // async
        response = base.request()
                .header("PreMatchFilter1", "async-pass")
                .header("PreMatchFilter2", "sync-pass")
                .header("PreMatchFilter3", "sync-pass")
                .get();
        Assertions.assertEquals("resource", response.readEntity(String.class));
        Assertions.assertEquals(200, response.getStatus());

        response = base.request()
                .header("PreMatchFilter1", "async-pass")
                .header("PreMatchFilter2", "async-pass")
                .header("PreMatchFilter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        response = base.request()
                .header("PreMatchFilter1", "async-pass")
                .header("PreMatchFilter2", "async-pass")
                .header("PreMatchFilter3", "async-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        response = base.request()
                .header("PreMatchFilter1", "async-pass")
                .header("PreMatchFilter2", "sync-pass")
                .header("PreMatchFilter3", "async-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        response = base.request()
                .header("PreMatchFilter1", "sync-pass")
                .header("PreMatchFilter2", "async-pass")
                .header("PreMatchFilter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        // async failures

        response = base.request()
                .header("PreMatchFilter1", "async-fail")
                .header("PreMatchFilter2", "sync-fail")
                .header("PreMatchFilter3", "sync-fail")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("PreMatchFilter1", response.readEntity(String.class));

        response = base.request()
                .header("PreMatchFilter1", "async-pass")
                .header("PreMatchFilter2", "sync-fail")
                .header("PreMatchFilter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("PreMatchFilter2", response.readEntity(String.class));

        response = base.request()
                .header("PreMatchFilter1", "async-pass")
                .header("PreMatchFilter2", "async-fail")
                .header("PreMatchFilter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("PreMatchFilter2", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Interceptors work
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testResponseFilters() throws Exception {

        // all sync

        Response response = base.request()
                .header("ResponseFilter1", "sync-pass")
                .header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        response = base.request()
                .header("ResponseFilter1", "sync-fail")
                .header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("ResponseFilter1", response.readEntity(String.class));

        response = base.request()
                .header("ResponseFilter1", "sync-pass")
                .header("ResponseFilter2", "sync-fail")
                .header("ResponseFilter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("ResponseFilter2", response.readEntity(String.class));

        response = base.request()
                .header("ResponseFilter1", "sync-pass")
                .header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "sync-fail")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("ResponseFilter3", response.readEntity(String.class));

        // async
        response = base.request()
                .header("ResponseFilter1", "async-pass")
                .header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "sync-pass")
                .get();
        Assertions.assertEquals("resource", response.readEntity(String.class));
        Assertions.assertEquals(200, response.getStatus());

        response = base.request()
                .header("ResponseFilter1", "async-pass")
                .header("ResponseFilter2", "async-pass")
                .header("ResponseFilter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        response = base.request()
                .header("ResponseFilter1", "async-pass")
                .header("ResponseFilter2", "async-pass")
                .header("ResponseFilter3", "async-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        response = base.request()
                .header("ResponseFilter1", "async-pass")
                .header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "async-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        response = base.request()
                .header("ResponseFilter1", "sync-pass")
                .header("ResponseFilter2", "async-pass")
                .header("ResponseFilter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        // async failures

        response = base.request()
                .header("ResponseFilter1", "async-fail")
                .header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("ResponseFilter1", response.readEntity(String.class));

        response = base.request()
                .header("ResponseFilter1", "async-pass")
                .header("ResponseFilter2", "sync-fail")
                .header("ResponseFilter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("ResponseFilter2", response.readEntity(String.class));

        response = base.request()
                .header("ResponseFilter1", "async-pass")
                .header("ResponseFilter2", "async-fail")
                .header("ResponseFilter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("ResponseFilter2", response.readEntity(String.class));

        // async instantaneous
        response = base.request()
                .header("ResponseFilter1", "async-pass-instant")
                .header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));

        response = base.request()
                .header("ResponseFilter1", "async-fail-instant")
                .header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("ResponseFilter1", response.readEntity(String.class));
    }

    /**
     * @tpTestDetails Interceptors work
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testResponseFilters2() throws Exception {

        // async way later
        Response response = base.request()
                .header("ResponseFilter1", "sync-pass")
                .header("ResponseFilter2", "sync-pass")
                .header("ResponseFilter3", "async-fail-late")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("ResponseFilter3", response.readEntity(String.class));
    }

    @Test
    void responseFiltersThrowCallbackAsync(@RestResource @RequestPath("callback-async") final WebTarget target) {
        testResponseFilterThrow(target, false);
    }

    @Test
    void responseFiltersThrowCallback(@RestResource @RequestPath("callback") final WebTarget target) {
        testResponseFilterThrow(target, false);
    }

    @Test
    void responseFiltersThrowCallbackAsyncExceptionManager(
            @RestResource @RequestPath("callback-async") final WebTarget target) {
        testResponseFilterThrow(target, true);
    }

    @Test
    void responseFiltersThrowCallbackExceptionManager(@RestResource @RequestPath("callback") final WebTarget target) {
        testResponseFilterThrow(target, true);
    }

    private void testResponseFilterThrow(final WebTarget base, final boolean useExceptionMapper) {
        // throw in response filter
        Response response = base.request()
                .header("ResponseFilter1", "sync-pass")
                .header("ResponseFilter2", "sync-pass")
                .header("UseExceptionMapper", useExceptionMapper)
                .header("ResponseFilter3", "async-throw-late")
                .get();
        // this is 500 even with exception mapper because exceptions in response filters are not mapped
        Assertions.assertEquals(500, response.getStatus());

        try {
            // give a chance to CI to run the callbacks
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        // check that callbacks were called
        response = base.request().get();
        Assertions.assertEquals(200, response.getStatus());
        if (useExceptionMapper)
            Assertions.assertEquals("dev.resteasy.server.vertx.async.AsyncFilterException: ouch",
                    response.getHeaders().getFirst("ResponseFilterCallbackResponseFilter3"));
        else
            Assertions.assertEquals("java.lang.Throwable: ouch",
                    response.getHeaders().getFirst("ResponseFilterCallbackResponseFilter3"));

        // throw in request filter
        response = base.request()
                .header("Filter1", "sync-pass")
                .header("Filter2", "sync-pass")
                .header("UseExceptionMapper", useExceptionMapper)
                .header("Filter3", "async-throw-late")
                .get();
        if (useExceptionMapper) {
            Assertions.assertEquals(Status.ACCEPTED.getStatusCode(), response.getStatus());
            Assertions.assertEquals("exception was mapped", response.readEntity(String.class));
        } else {
            Assertions.assertEquals(500, response.getStatus());
        }

        try {
            // give a chance to CI to run the callbacks
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        // check that callbacks were called
        response = base.request().get();
        Assertions.assertEquals(200, response.getStatus());
        if (useExceptionMapper)
            Assertions.assertEquals("dev.resteasy.server.vertx.async.AsyncFilterException: ouch",
                    response.getHeaders().getFirst("RequestFilterCallbackFilter3"));
        else
            Assertions.assertEquals("java.lang.Throwable: ouch",
                    response.getHeaders().getFirst("RequestFilterCallbackFilter3"));

    }

    /**
     * @tpTestDetails Interceptors work with non-Response resource methods
     * @tpSince RESTEasy 4.0.0
     */
    @Test
    public void testRequestFiltersGuessReturnType(@RestResource @RequestPath("/non-response") final WebTarget base)
            throws Exception {
        Response response = base.request()
                .header("Filter1", "async-pass")
                .header("Filter2", "sync-pass")
                .header("Filter3", "sync-pass")
                .get();
        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("resource", response.readEntity(String.class));
    }
}
