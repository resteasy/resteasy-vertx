/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.build.compatible.spi.Parameters;
import jakarta.enterprise.inject.build.compatible.spi.Synthesis;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanCreator;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticBeanDisposer;
import jakarta.enterprise.inject.build.compatible.spi.SyntheticComponents;

import io.vertx.core.Vertx;

import dev.resteasy.vertx.VertxManager;

/**
 * A {@linkplain BuildCompatibleExtension CDI extension} that registers a synthetic {@link Vertx} bean.
 * <p>
 * The synthetic bean delegates to {@link VertxManager#get()} to obtain the shared {@link Vertx} instance,
 * ensuring that CDI-injected {@code Vertx} instances use the same managed instance as the server and client.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class VertxCdiBuildCompatibleExtension implements BuildCompatibleExtension {

    @Synthesis
    public void registerVertxProducer(final SyntheticComponents syntheticComponents) {
        syntheticComponents.addBean(Vertx.class)
                .type(Vertx.class)
                .scope(ApplicationScoped.class)
                .qualifier(Default.class)
                .createWith(VertxSyntheticBean.class)
                .disposeWith(VertxSyntheticBean.class);
    }

    /**
     * Creates the synthetic {@link Vertx} bean by delegating to the {@link VertxManager}.
     */
    public static class VertxSyntheticBean implements SyntheticBeanCreator<Vertx>, SyntheticBeanDisposer<Vertx> {

        @Override
        public Vertx create(final Instance<Object> lookup, final Parameters params) {
            return VertxManager.get().vertx();
        }

        @Override
        public void dispose(final Vertx instance, final Instance<Object> lookup, final Parameters params) {
            VertxManager.get().close();
        }
    }
}
