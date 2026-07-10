/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.client.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.test.fakestream.FakeStream;

public class InputStreamAdapterTest {

    private Vertx vertx;

    @BeforeEach
    public void setup() {
        vertx = Vertx.vertx();
    }

    @AfterEach
    public void after() {
        vertx.close();
    }

    @Test
    public void testConsumeSingleByteWaitsUntilDataBecomesAvailable() throws Exception {
        FakeStream<Buffer> stream = new FakeStream<>();
        VertxInputStream adapter = new VertxInputStream(stream);
        Thread th = Thread.currentThread();
        vertx.setTimer(10, id -> {
            while (th.getState() != Thread.State.WAITING) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignore) {
                }
            }
            stream.emit(Buffer.buffer().appendByte((byte) 5));
        });
        int val = adapter.read();
        assertEquals(5, val);
    }

    @Test
    public void testPauseStreamStream() throws Exception {
        FakeStream<Buffer> stream = new FakeStream<>();
        // Use explicit maxPendingSize of 256 to test pause/resume behavior
        VertxInputStream adapter = new VertxInputStream(stream, 256);
        final byte[] line = new byte[256 + 1];
        ThreadLocalRandom.current().nextBytes(line);
        Buffer expected = Buffer.buffer(line);
        stream.emit(expected.slice(0, 256));
        assertFalse(stream.isPaused());
        stream.emit(expected.slice(256, 257));
        assertTrue(stream.isPaused());
        byte[] data = new byte[257];
        assertEquals(257, adapter.read(data));
        assertFalse(stream.isPaused());
    }

    @Test
    public void testEndStream1() throws Exception {
        FakeStream<Buffer> stream = new FakeStream<>();
        VertxInputStream adapter = new VertxInputStream(stream);
        stream.end();
        assertEquals(-1, adapter.read());
    }

    @Test
    public void testEndStream2() throws Exception {
        FakeStream<Buffer> stream = new FakeStream<>();
        VertxInputStream adapter = new VertxInputStream(stream);
        Thread th = Thread.currentThread();
        vertx.setTimer(10, id -> {
            while (th.getState() != Thread.State.WAITING) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignore) {
                }
            }
            stream.end();
        });
        assertEquals(-1, adapter.read());
    }

    @Test
    public void testFailure1() throws Exception {
        FakeStream<Buffer> stream = new FakeStream<>();
        VertxInputStream adapter = new VertxInputStream(stream);
        Throwable cause = new Throwable();
        stream.fail(cause);
        try {
            adapter.read();
        } catch (IOException e) {
            assertSame(cause, e.getCause());
        }
    }

    @Test
    public void testFailure2() throws Exception {
        FakeStream<Buffer> stream = new FakeStream<>();
        VertxInputStream adapter = new VertxInputStream(stream);
        Throwable cause = new Throwable();
        Thread th = Thread.currentThread();
        vertx.setTimer(10, id -> {
            while (th.getState() != Thread.State.WAITING) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignore) {
                }
            }
            stream.fail(cause);
        });
        try {
            adapter.read();
        } catch (IOException e) {
            assertSame(cause, e.getCause());
        }
    }
}
