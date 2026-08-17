/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx;

import java.util.Comparator;
import java.util.ServiceLoader;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
class VertxManagerHolder {
    static final VertxManager INSTANCE;

    static {
        INSTANCE = ServiceLoader.load(VertxManager.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .min(Comparator.comparingInt(VertxManager::priority))
                .orElseGet(DefaultVertxManager::new);
    }
}
