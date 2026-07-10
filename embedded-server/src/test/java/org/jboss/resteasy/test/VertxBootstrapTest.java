/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.resteasy.test;

import org.jboss.resteasy.bootstrap.test.SeBootstrapTest;
import org.jboss.resteasy.plugins.server.embedded.EmbeddedServer;
import org.jboss.resteasy.plugins.server.vertx.VertxJaxrsServer;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class VertxBootstrapTest extends SeBootstrapTest {
    @Override
    protected Class<? extends EmbeddedServer> getEmbeddedServerClass() {
        return VertxJaxrsServer.class;
    }
}
