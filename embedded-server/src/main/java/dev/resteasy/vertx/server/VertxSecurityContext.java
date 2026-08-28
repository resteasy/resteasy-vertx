/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package dev.resteasy.vertx.server;

import java.security.Principal;

import jakarta.ws.rs.core.SecurityContext;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class VertxSecurityContext implements SecurityContext {
    protected final Principal principal;
    protected final boolean isSecure;

    public VertxSecurityContext(final String username, final boolean secure) {
        this.principal = username != null ? () -> username : null;
        this.isSecure = secure;
    }

    public VertxSecurityContext() {
        this(null, false);
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public boolean isSecure() {
        return isSecure;
    }

    @Override
    public String getAuthenticationScheme() {
        return null;
    }
}
