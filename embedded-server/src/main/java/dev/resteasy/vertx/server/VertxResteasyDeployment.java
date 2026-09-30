/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.server;

import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.spi.Dispatcher;

import dev.resteasy.vertx.server._private.VertxLogger;

/**
 * A {@link ResteasyDeploymentImpl} that installs a {@link VertxSynchronousDispatcher} in place of the
 * default {@link org.jboss.resteasy.core.SynchronousDispatcher}, unless a dispatcher has already been
 * configured.
 * <p>
 * The {@link #setDispatcher(Dispatcher)} will throw a {@link UnsupportedOperationException} if invoked.
 * The Vert.x integration must use its own dispatcher.
 * </p>
 * <p>
 * The legacy asynchronous job service (JAX-RS 1.x style polling) is not supported: it requires an
 * {@link org.jboss.resteasy.core.AsynchronousDispatcher}, which is incompatible with the
 * {@link VertxSynchronousDispatcher} this deployment always installs.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class VertxResteasyDeployment extends ResteasyDeploymentImpl {

    /**
     * Creates a new deployment
     */
    public VertxResteasyDeployment() {
        super.asyncJobServiceEnabled = false;
    }

    @Override
    public void setAsyncJobServiceEnabled(final boolean asyncJobServiceEnabled) {
        if (asyncJobServiceEnabled) {
            throw VertxLogger.LOGGER.asyncJobServiceNotSupported();
        }
        super.setAsyncJobServiceEnabled(false);
    }

    @Override
    public void setDispatcher(final Dispatcher dispatcher) {
        throw VertxLogger.LOGGER.settingDispatcherNotSupported();
    }

    @Override
    protected void initializeDispatcher() {
        if (this.dispatcher == null) {
            dispatcher = new VertxSynchronousDispatcher(getProviderFactory());
        }
        super.initializeDispatcher();
    }
}
