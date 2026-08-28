/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.server.async;

import jakarta.annotation.Priority;
import jakarta.ws.rs.ext.Provider;

@Priority(1)
@Provider
public class AsyncResponseFilter1 extends AsyncResponseFilter {

    public AsyncResponseFilter1() {
        super("ResponseFilter1");
    }
}
