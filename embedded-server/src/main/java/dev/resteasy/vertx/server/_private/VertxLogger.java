/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.server._private;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 */
@MessageLogger(projectCode = "RESTEASY-VERTX")
public interface VertxLogger extends BasicLogger {
    VertxLogger LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), VertxLogger.class,
            VertxLogger.class.getPackage().getName());

    // Exceptions
    @Message(id = 1000, value = "The response has already been committed, cannot reset.")
    IllegalStateException alreadyCommitted();

    @Message(id = 1020, value = "Already suspended")
    IllegalStateException alreadySuspended();

    @Message(id = 1030, value = "Chunk size must be at least 1")
    IllegalArgumentException chunkSizeMustBeAtLeastOne();

    @Message(id = 1040, value = "Server has already been started")
    IllegalStateException serverAlreadyStarted();

    @Message(id = 1050, value = "Failed to start the server")
    RuntimeException failedToStartServer(@Cause Throwable cause);

    @Message(id = 1060, value = "Failed to shutdown HTTP server within %d %s")
    RuntimeException failedToShutdownServer(@Cause Throwable cause, long timeout, String unit);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 5000, value = "Failed to handle request")
    void failedRequest(@Cause Throwable e);
}
