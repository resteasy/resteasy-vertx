/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx._private;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "RESTEASY-VERTX-API")
public interface VertxMessages {

    VertxMessages MESSAGES = Messages.getBundle(MethodHandles.lookup(), VertxMessages.class);

    @Message(id = 100, value = "Failed to shutdown Vertx instance within %d %s")
    RuntimeException failedToShutdownVertxWithin(@Cause Throwable cause, long timeout, String unit);
}
