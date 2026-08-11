/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.server.vertx.async;

@SuppressWarnings("serial")
public class AsyncFilterException extends RuntimeException {

    public AsyncFilterException(final String message) {
        super(message);
    }

}
