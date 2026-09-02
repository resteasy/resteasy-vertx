/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Test module for integration testing
 */
open module dev.resteasy.vertx.it {
    requires jakarta.cdi;
    requires jakarta.ws.rs;

    // Vert.x modules
    requires io.vertx.core;
    requires io.vertx.web;

    // Project Modules
    requires dev.resteasy.vertx.cdi;
    requires dev.resteasy.vertx.client;
    requires dev.resteasy.vertx.server;

    // Test modules
    requires org.junit.jupiter.api;
    requires dev.resteasy.junit.extension;
}
