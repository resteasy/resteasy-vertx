/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.cdi;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.jboss.resteasy.core.ResteasyDeploymentImpl;
import org.jboss.resteasy.spi.DelegateResteasyDeployment;

class CdiResteasyDeployment extends DelegateResteasyDeployment {

    private final Lock lock = new ReentrantLock();
    private boolean started = false;

    CdiResteasyDeployment() {
        super(new ResteasyDeploymentImpl());
    }

    @Override
    public void start() {
        // Only start once
        lock.lock();
        try {
            if (started) {
                return;
            }
            super.setInjectorFactory(new CdiInjectorFactory(ManagedSeContainer.instance().getBeanManager()));
            super.start();
            started = true;
        } catch (Exception e) {
            started = false;
            try {
                super.stop();
            } catch (Exception ex) {
                e.addSuppressed(ex);
            }
            throw e;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void stop() {
        lock.lock();
        try {
            try {
                super.stop();
            } finally {
                started = false;
            }
        } finally {
            lock.unlock();
        }
    }
}
