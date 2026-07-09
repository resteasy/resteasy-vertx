/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.resteasy.test.async;

import jakarta.annotation.Priority;
import jakarta.ws.rs.ext.Provider;

@Priority(2)
@Provider
public class AsyncRequestFilter2 extends AsyncRequestFilter {

    public AsyncRequestFilter2() {
        super("Filter2");
    }
}
