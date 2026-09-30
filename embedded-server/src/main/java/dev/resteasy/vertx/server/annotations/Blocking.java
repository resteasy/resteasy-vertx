/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.vertx.server.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that invocation of the resource methods are blocking.
 * <p>
 * When placed on a type, then all methods in the resource are considered blocking. When placed on a method, then
 * the annotated method is considered blocking. This overrides the default detection based on the method's return
 * type or parameters, which is useful when a method returns a {@link java.util.concurrent.CompletionStage} or takes
 * a {@link jakarta.ws.rs.container.Suspended @Suspended} parameter but still performs blocking work before it
 * returns.
 * </p>
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Blocking {
}
