/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.server;

import org.jboss.resteasy.bootstrap.test.SeBootstrapTest;
import org.jboss.resteasy.plugins.server.embedded.EmbeddedServer;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class VertxBootstrapTest extends SeBootstrapTest {
    @Override
    protected Class<? extends EmbeddedServer> getEmbeddedServerClass() {
        return VertxEmbeddedServer.class;
    }
}
