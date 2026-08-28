/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.server.asyncio;

public class AsyncThrowingWriterData {
    public boolean throwNow;

    public AsyncThrowingWriterData(final boolean throwNow) {
        this.throwNow = throwNow;
    }
}
