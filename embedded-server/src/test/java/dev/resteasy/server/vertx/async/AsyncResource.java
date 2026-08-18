/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.server.vertx.async;

import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.vertx.core.Vertx;

@Path("/async")
public class AsyncResource {
    @GET
    @Produces("text/plain")
    public void get(@Context final Vertx vertx, @Suspended final AsyncResponse response) {
        response.setTimeout(2000, TimeUnit.MILLISECONDS);
        vertx.executeBlocking(() -> {
            Thread.sleep(100);
            return Response.ok("hello").type(MediaType.TEXT_PLAIN).build();
        }, false).onSuccess(response::resume)
                .onFailure(response::resume);
    }

    @GET
    @Path("throw")
    @Produces("text/plain")
    public void throwWhileSuspended(@Suspended final AsyncResponse response) {
        throw new WebApplicationException("exception without resume");
    }

    @GET
    @Path("timeout")
    @Produces("text/plain")
    public void timeout(@Context final Vertx vertx, @Suspended final AsyncResponse response) {
        response.setTimeout(10, TimeUnit.MILLISECONDS);
        vertx.executeBlocking(() -> {
            Thread.sleep(1000);
            return Response.ok("goodbye").type(MediaType.TEXT_PLAIN).build();
        }, false).onSuccess(response::resume)
                .onFailure(response::resume);
    }
}
