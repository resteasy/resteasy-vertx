/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.util.StringContextReplacement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import dev.resteasy.junit.extension.annotations.RequestPath;
import dev.resteasy.junit.extension.annotations.RestBootstrap;
import dev.resteasy.junit.extension.annotations.RestResource;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@RestBootstrap(VertxTest.Resource.class)
public class VertxTest {

    @Path("/")
    public static class Resource {
        @GET
        @Path("/test")
        @Produces("text/plain")
        public String hello() {
            return "hello world";
        }

        @GET
        @Path("empty")
        public void empty() {

        }

        @GET
        @Path("query")
        public String query(@QueryParam("param") String value) {
            return value;

        }

        @GET
        @Path("/exception")
        @Produces("text/plain")
        public String exception() {
            throw new RuntimeException();
        }

        @GET
        @Path("large")
        @Produces("text/plain")
        public String large() {
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < 1000; i++) {
                buf.append(i);
            }
            return buf.toString();
        }

        @GET
        @Path("/context")
        @Produces("text/plain")
        public String context(
                @Context io.vertx.core.Context context,
                @Context io.vertx.core.Vertx vertx,
                @Context io.vertx.core.http.HttpServerRequest req,
                @Context io.vertx.core.http.HttpServerResponse resp) {
            if (context != null && vertx != null && req != null && resp != null) {
                return "pass";
            } else {
                return "fail";
            }
        }

        @POST
        @Path("/post")
        @Produces("text/plain")
        public String post(String postBody) {
            return postBody;
        }

        @GET
        @Path("/test/absolute")
        @Produces("text/plain")
        public String absolute(@Context UriInfo info) {
            return "uri: " + info.getRequestUri().toString();
        }

        @POST
        @Path("/replace")
        @Produces("text/plain")
        @Consumes("text/plain")
        public String replace(String replace) {
            return StringContextReplacement.replace(replace);
        }

        @GET
        @Path("request")
        @Produces("text/plain")
        public String getRequest(@Context HttpRequest req) {
            return req.getRemoteAddress() + "/" + req.getRemoteHost();
        }
    }

    @Test
    public void testBasic(@RestResource @RequestPath("test") final WebTarget target) throws Exception {
        String val = target.request().get(String.class);
        Assertions.assertEquals("hello world", val);
    }

    @Test
    public void testHeadContentLength(@RestResource @RequestPath("test") final WebTarget target) throws Exception {
        Response getResponse = target.request().buildGet().invoke();
        String val = ClientInvocation.extractResult(new GenericType<String>(String.class), getResponse, null);
        Assertions.assertEquals("hello world", val);
        Assertions.assertEquals("chunked", getResponse.getHeaderString("transfer-encoding"));
        Response headResponse = target.request().build(HttpMethod.HEAD).invoke();
        Assertions.assertNull(headResponse.getHeaderString("Content-Length"));
        Assertions.assertNull(headResponse.getHeaderString("transfer-encoding"));
    }

    @Test
    public void testQuery(@RestResource @RequestPath("query") final WebTarget target) throws Exception {
        String val = target.queryParam("param", "val").request().get(String.class);
        Assertions.assertEquals("val", val);
    }

    @Test
    public void testEmpty(@RestResource @RequestPath("empty") final WebTarget target) throws Exception {
        try (Response response = target.request().get()) {
            Assertions.assertEquals(204, response.getStatus());
        }
    }

    @Test
    public void testLarge(@RestResource @RequestPath("large") final WebTarget target) throws Exception {
        try (Response response = target.request().get()) {
            Assertions.assertEquals(200, response.getStatus());
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < 1000; i++) {
                buf.append(i);
            }
            String expected = buf.toString();
            String have = response.readEntity(String.class);
            Assertions.assertEquals(expected, have);

        }
    }

    @Test
    public void testUnhandledException(@RestResource @RequestPath("exception") final WebTarget target) throws Exception {
        try (Response resp = target.request().get()) {
            Assertions.assertEquals(500, resp.getStatus());
        }
    }

    @Test
    public void testChannelContext(@RestResource @RequestPath("context") final WebTarget target) throws Exception {
        String val = target.request().get(String.class);
        Assertions.assertEquals("pass", val);
    }

    @Test
    public void testReplacement(@RestResource @RequestPath("replace") final WebTarget target) throws Exception {
        // this test was put in to make sure that without servlet it still works.
        String val = target.request().post(Entity.text("${contextpath}"), String.class);
        Assertions.assertEquals("/", val);
    }

    @Test
    public void testPost(@RestResource @RequestPath("post") final WebTarget target) {
        String postBody = "hello world";
        String result = target.request().post(Entity.text(postBody), String.class);
        Assertions.assertEquals(postBody, result);
    }

    /**
     * Per the HTTP spec, we must allow requests like:
     * <p>
     *
     * <pre>
     *     GET http://www.example.com/content HTTP/1.1
     *     Host: www.example.com
     * </pre>
     * <p>
     * <blockquote>
     * RFC 2616 5.1.12:
     * To allow for transition to absoluteURIs in all requests in future
     * versions of HTTP, all HTTP/1.1 servers MUST accept the absoluteURI
     * form in requests, even though HTTP/1.1 clients will only generate
     * them in requests to proxies.
     * </blockquote>
     *
     * @throws Exception
     */
    @Test
    public void testAbsoluteURI(@RestResource @RequestPath("test/absolute") final URI uri) throws Exception {
        Socket client = new Socket(uri.getHost(), uri.getPort());
        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out.printf(Locale.US, "GET %s HTTP/1.1\r\nHost: %s:%d\r\n\r\n", uri, uri.getHost(), uri.getPort());
        String statusLine = in.readLine();
        String response = in.readLine();
        while (!response.startsWith("uri")) {
            response = in.readLine();
        }
        client.close();
        Assertions.assertEquals("HTTP/1.1 200 OK", statusLine);
        Assertions.assertEquals(uri.toString(), response.subSequence(5, response.length()));
    }

    @Test
    public void testRequest(@RestResource @RequestPath("request") final WebTarget target) throws Exception {
        String val = target.request().get(String.class);
        final String pattern = "^127.0.0.1/.+";
        Assertions.assertTrue(Pattern.matches(pattern, val),
                String.format("Expected value '%s' to match pattern '%s'", val, pattern));
    }
}
