/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.server;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.vertx.core.buffer.Buffer;

class BufferInputStreamTest {

    @Test
    void readSingleBytes() throws IOException {
        final Buffer buffer = Buffer.buffer(new byte[] { 1, 2, 3 });
        try (BufferInputStream in = new BufferInputStream(buffer)) {
            Assertions.assertEquals(1, in.read());
            Assertions.assertEquals(2, in.read());
            Assertions.assertEquals(3, in.read());
            Assertions.assertEquals(-1, in.read());
        }
    }

    @Test
    void readBulk() throws IOException {
        final byte[] data = "hello world".getBytes();
        final Buffer buffer = Buffer.buffer(data);
        try (BufferInputStream in = new BufferInputStream(buffer)) {
            final byte[] result = new byte[data.length];
            final int bytesRead = in.read(result, 0, result.length);
            Assertions.assertEquals(data.length, bytesRead);
            Assertions.assertArrayEquals(data, result);
            Assertions.assertEquals(-1, in.read(result, 0, result.length));
        }
    }

    @Test
    void readBulkPartial() throws IOException {
        final Buffer buffer = Buffer.buffer(new byte[] { 10, 20, 30, 40, 50 });
        try (BufferInputStream in = new BufferInputStream(buffer)) {
            final byte[] result = new byte[3];
            Assertions.assertEquals(3, in.read(result, 0, 3));
            Assertions.assertArrayEquals(new byte[] { 10, 20, 30 }, result);
            Assertions.assertEquals(2, in.read(result, 0, 3));
            Assertions.assertArrayEquals(new byte[] { 40, 50, 30 }, result);
        }
    }

    @Test
    void emptyBuffer() throws IOException {
        final Buffer buffer = Buffer.buffer();
        try (BufferInputStream in = new BufferInputStream(buffer)) {
            Assertions.assertEquals(-1, in.read());
            Assertions.assertEquals(0, in.available());
        }
    }

    @Test
    void available() throws IOException {
        final Buffer buffer = Buffer.buffer(new byte[] { 1, 2, 3, 4, 5 });
        try (BufferInputStream in = new BufferInputStream(buffer)) {
            Assertions.assertEquals(5, in.available());
            Assertions.assertEquals(1, in.read());
            Assertions.assertEquals(4, in.available());
            final byte[] threeBuff = new byte[3];
            Assertions.assertEquals(3, in.read(threeBuff));
            Assertions.assertArrayEquals(new byte[] { 2, 3, 4 }, threeBuff);
            Assertions.assertEquals(1, in.available());
        }
    }

    @Test
    void skip() throws IOException {
        final Buffer buffer = Buffer.buffer(new byte[] { 1, 2, 3, 4, 5 });
        try (BufferInputStream in = new BufferInputStream(buffer)) {
            Assertions.assertEquals(2, in.skip(2));
            Assertions.assertEquals(3, in.read());
            Assertions.assertEquals(2, in.skip(10));
            Assertions.assertEquals(-1, in.read());
        }
    }

    @Test
    void markAndReset() throws IOException {
        final Buffer buffer = Buffer.buffer(new byte[] { 1, 2, 3, 4, 5 });
        try (BufferInputStream in = new BufferInputStream(buffer)) {
            Assertions.assertTrue(in.markSupported());
            Assertions.assertEquals(1, in.read());
            in.mark(0);
            Assertions.assertEquals(2, in.read());
            Assertions.assertEquals(3, in.read());
            in.reset();
            Assertions.assertEquals(2, in.read());
        }
    }

    @Test
    void unsignedByteValues() throws IOException {
        final Buffer buffer = Buffer.buffer(new byte[] { (byte) 0xFF, (byte) 0x80, 0x00 });
        try (BufferInputStream in = new BufferInputStream(buffer)) {
            Assertions.assertEquals(255, in.read());
            Assertions.assertEquals(128, in.read());
            Assertions.assertEquals(0, in.read());
        }
    }
}
