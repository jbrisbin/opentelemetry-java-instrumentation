/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.aspects;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.api.annotation.support.MethodSpanAttributesExtractor;
import io.opentelemetry.instrumentation.api.annotation.support.ParameterAttributeNamesExtractor;
import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndSupport;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.code.CodeAttributesExtractor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.ParameterNameDiscoverer;

/**
 * Uses Spring-AOP to wrap methods marked by {@link WithSpan} (or the deprecated {@link
 * io.opentelemetry.extension.annotations.WithSpan}) in a {@link Span}.
 *
 * <p>Ensure methods annotated with {@link WithSpan} are implemented on beans managed by the Spring
 * container.
 *
 * <p>Note: This Aspect uses spring-aop to proxy beans. Therefore, the {@link WithSpan} annotation
 * can not be applied to constructors.
 */
@Aspect
public class WithSpanAspect {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.spring-boot-autoconfigure";

  private final Instrumenter<JoinPointRequest, Object> instrumenter;

  public WithSpanAspect(
      OpenTelemetry openTelemetry, ParameterNameDiscoverer parameterNameDiscoverer) {

    ParameterAttributeNamesExtractor parameterAttributeNamesExtractor =
        new WithSpanAspectParameterAttributeNamesExtractor(parameterNameDiscoverer);

    instrumenter =
        Instrumenter.builder(openTelemetry, INSTRUMENTATION_NAME, JoinPointRequest::spanName)
            .addAttributesExtractor(
                CodeAttributesExtractor.create(JointPointCodeAttributesExtractor.INSTANCE))
            .addAttributesExtractor(
                MethodSpanAttributesExtractor.newInstance(
                    JoinPointRequest::method,
                    parameterAttributeNamesExtractor,
                    JoinPointRequest::args))
            .buildInstrumenter(JoinPointRequest::spanKind);
  }

  @Around(
      "@annotation(io.opentelemetry.extension.annotations.WithSpan) || "
          + "@annotation(io.opentelemetry.instrumentation.annotations.WithSpan)")
  public Object traceMethod(ProceedingJoinPoint pjp) throws Throwable {

    JoinPointRequest request = new JoinPointRequest(pjp);
    Context parentContext = Context.current();
    if (!instrumenter.shouldStart(parentContext, request)) {
      return pjp.proceed();
    }

    Context context = instrumenter.start(parentContext, request);
    AsyncOperationEndSupport<JoinPointRequest, Object> asyncOperationEndSupport =
        AsyncOperationEndSupport.create(
            instrumenter, Object.class, request.method().getReturnType());
    try (Scope ignored = context.makeCurrent()) {
      Object response = pjp.proceed();
      return asyncOperationEndSupport.asyncEnd(context, request, response, null);
    } catch (Throwable t) {
      asyncOperationEndSupport.asyncEnd(context, request, null, t);
      throw t;
    }
  }
}
