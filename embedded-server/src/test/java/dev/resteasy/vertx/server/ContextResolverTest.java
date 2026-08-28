/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.server;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.Providers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

/**
 * Test that dynamic feature doesn't add to all resource methods
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@RestBootstrap({ ContextResolverTest.HolderResolver.class, ContextResolverTest.Resource.class })
public class ContextResolverTest {
    public static class HolderClass {
        public static final String OK = "11111";

        private HttpHeaders headers;

        private UriInfo info;

        private Application application;

        private Request request;

        private Providers provider;

        public HolderClass(final HttpHeaders headers, final UriInfo info, final Application application,
                final Request request, final Providers provider) {
            super();
            this.headers = headers;
            this.info = info;
            this.application = application;
            this.request = request;
            this.provider = provider;
        }

        public Response toResponse() {
            int ok = application != null ? 1 : 0;
            ok += headers != null ? 10 : 0;
            ok += info != null ? 100 : 0;
            ok += request != null ? 1000 : 0;
            ok += provider == null ? 10000 : 0;
            return Response.ok(String.valueOf(ok)).build();
        }
    }

    @Provider
    public static class HolderResolver implements ContextResolver<HolderClass> {
        private HttpHeaders headers;

        private UriInfo info;

        private Application application;

        private Request request;

        private Providers provider;

        protected HolderResolver(final @Context HttpHeaders headers, final @Context UriInfo info,
                final @Context Application application, final @Context Request request,
                final @Context Providers provider) {
            super();
            this.headers = headers;
            this.info = info;
            this.application = application;
            this.request = request;
            this.provider = provider;
        }

        public HolderResolver(final @Context HttpHeaders headers, final @Context UriInfo info,
                final @Context Application application, final @Context Request request) {
            super();
            this.headers = headers;
            this.info = info;
            this.application = application;
            this.request = request;
        }

        public HolderResolver(final @Context HttpHeaders headers, final @Context UriInfo info,
                final @Context Application application) {
            super();
            this.headers = headers;
            this.info = info;
            this.application = application;
        }

        public HolderResolver(final @Context HttpHeaders headers, final @Context UriInfo info) {
            super();
            this.headers = headers;
            this.info = info;
        }

        public HolderResolver(final @Context HttpHeaders headers) {
            super();
            this.headers = headers;
        }

        @Override
        public HolderClass getContext(Class<?> type) {
            return new HolderClass(headers, info, application, request, provider);
        }
    }

    @Path("resource")
    public static class Resource {

        @Path("contextresolver")
        @GET
        public Response contextresolver(@Context Providers providers) {
            ContextResolver<HolderClass> holder = providers
                    .getContextResolver(HolderClass.class, MediaType.WILDCARD_TYPE);
            return holder.getContext(HolderClass.class).toResponse();
        }
    }

    @Test
    public void testBasic(@RestResource @RequestPath("/resource/contextresolver") final WebTarget target) throws Exception {
        String val = target.request().get(String.class);
        // Minor note on why this changed from 11110 to 11111. Previously this test did not use an Application so the
        // Application was null, that is no longer the case.
        Assertions.assertEquals("11111", val);
    }
}
