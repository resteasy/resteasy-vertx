package org.jboss.resteasy.plugins.server.vertx.i18n;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 */
@MessageBundle(projectCode = "RESTEASY-VERTX")
public interface Messages {
    Messages MESSAGES = org.jboss.logging.Messages.getBundle(MethodHandles.lookup(), Messages.class);

    @Message(id = 10, value = "Already committed")
    String alreadyCommitted();

    @Message(id = 20, value = "Already suspended")
    String alreadySuspended();

    @Message(id = 30, value = "Chunk size must be at least 1")
    String chunkSizeMustBeAtLeastOne();

    @Message(id = 40, value = "response is committed")
    String responseIsCommitted();

    @Message(id = 50, value = "Unexpected")
    String unexpected();
}
