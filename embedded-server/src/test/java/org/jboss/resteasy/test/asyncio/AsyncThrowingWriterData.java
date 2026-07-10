/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.resteasy.test.asyncio;

public class AsyncThrowingWriterData {
    public boolean throwNow;

    public AsyncThrowingWriterData(final boolean throwNow) {
        this.throwNow = throwNow;
    }
}
