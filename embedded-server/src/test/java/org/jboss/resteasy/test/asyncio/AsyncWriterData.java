package org.jboss.resteasy.test.asyncio;

public class AsyncWriterData {

    public final boolean simulateSlowIo;
    public final String expectedValue;

    public AsyncWriterData(final boolean simulateSlowIo, final String expectedValue) {
        this.simulateSlowIo = simulateSlowIo;
        this.expectedValue = expectedValue;
    }

}
