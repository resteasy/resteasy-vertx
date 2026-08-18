/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.server.vertx.asyncio;

import java.io.IOException;
import java.io.UncheckedIOException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import io.vertx.core.Vertx;

@Path("close")
public class SSEResource {

    private static volatile boolean exception = false;

    private static volatile boolean isClosed = false;

    @GET
    @Path("reset")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void reset(@Context SseEventSink sink, @Context Sse sse) {
        exception = false;
        isClosed = false;
        sink.send(sse.newEvent("RESET"));
    }

    @GET
    @Path("send")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void send(@Context Vertx vertx, @Context SseEventSink sink, @Context Sse sse) {
        vertx.executeBlocking(() -> {
            try {
                SseEventSink s = sink;
                s.send(sse.newEvent("HELLO"));
                s.close();
                isClosed = s.isClosed();
                if (!isClosed)
                    return null;
                s.close();
                isClosed = s.isClosed();
                if (!isClosed)
                    return null;
                s.close();
                isClosed = s.isClosed();
                if (!isClosed)
                    return null;
                try {
                    s.send(sse.newEvent("SOMETHING")).exceptionally(t -> {
                        if (t instanceof IllegalStateException)
                            exception = true;
                        return null;
                    });
                } catch (IllegalStateException ise) {
                    exception = true;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            return null;
        }).onFailure(t -> exception = true);
    }

    @GET
    @Path("check")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    public void check(@Context SseEventSink sink, @Context Sse sse) {
        if (!isClosed) {
            sink.send(sse.newEvent("Not closed"));
            return;
        }
        if (!exception) {
            sink.send(sse.newEvent("No IllegalStateException is thrown"));
            return;
        }
        sink.send(sse.newEvent("CHECK"));
    }

    @GET
    @Path("closed")
    @Produces(MediaType.TEXT_PLAIN)
    public boolean isClosed() {
        return isClosed;
    }
}
