/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.http;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.OperationListener;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class HttpClientMetricsTest {

  @Test
  void collectsMetrics() {
    InMemoryMetricReader metricReader = InMemoryMetricReader.create();
    SdkMeterProvider meterProvider =
        SdkMeterProvider.builder().registerMetricReader(metricReader).build();

    OperationListener listener = HttpClientMetrics.get().create(meterProvider.get("test"));

    Attributes requestAttributes =
        Attributes.builder()
            .put("http.method", "GET")
            .put("http.url", "https://localhost:1234/")
            .put("http.host", "host")
            .put("http.target", "/")
            .put("http.scheme", "https")
            .put("net.peer.name", "localhost")
            .put("net.peer.ip", "0.0.0.0")
            .put("net.peer.port", 1234)
            .put("http.request_content_length", 100)
            .build();

    Attributes responseAttributes =
        Attributes.builder()
            .put("http.flavor", "2.0")
            .put("http.server_name", "server")
            .put("http.status_code", 200)
            .put("http.response_content_length", 200)
            .build();

    Context parent =
        Context.root()
            .with(
                Span.wrap(
                    SpanContext.create(
                        "ff01020304050600ff0a0b0c0d0e0f00",
                        "090a0b0c0d0e0f00",
                        TraceFlags.getSampled(),
                        TraceState.getDefault())));

    Context context1 = listener.onStart(parent, requestAttributes, nanos(100));

    assertThat(metricReader.collectAllMetrics()).isEmpty();

    Context context2 = listener.onStart(Context.root(), requestAttributes, nanos(150));

    assertThat(metricReader.collectAllMetrics()).isEmpty();

    listener.onEnd(context1, responseAttributes, nanos(250));

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("http.client.duration")
                    .hasUnit("ms")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(150 /* millis */)
                                        .hasAttributesSatisfying(
                                            equalTo(SemanticAttributes.NET_PEER_NAME, "localhost"),
                                            equalTo(SemanticAttributes.NET_PEER_PORT, 1234),
                                            equalTo(SemanticAttributes.HTTP_METHOD, "GET"),
                                            equalTo(SemanticAttributes.HTTP_FLAVOR, "2.0"),
                                            equalTo(SemanticAttributes.HTTP_STATUS_CODE, 200))
                                        .hasExemplarsSatisfying(
                                            exemplar ->
                                                exemplar
                                                    .hasTraceId("ff01020304050600ff0a0b0c0d0e0f00")
                                                    .hasSpanId("090a0b0c0d0e0f00")))),
            metric ->
                assertThat(metric)
                    .hasName("http.client.request.size")
                    .hasUnit("By")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(100 /* bytes */)
                                        .hasAttributesSatisfying(
                                            equalTo(SemanticAttributes.NET_PEER_NAME, "localhost"),
                                            equalTo(SemanticAttributes.NET_PEER_PORT, 1234),
                                            equalTo(SemanticAttributes.HTTP_METHOD, "GET"),
                                            equalTo(SemanticAttributes.HTTP_FLAVOR, "2.0"),
                                            equalTo(SemanticAttributes.HTTP_STATUS_CODE, 200)))),
            metric ->
                assertThat(metric)
                    .hasName("http.client.response.size")
                    .hasUnit("By")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(200 /* bytes */)
                                        .hasAttributesSatisfying(
                                            equalTo(SemanticAttributes.NET_PEER_NAME, "localhost"),
                                            equalTo(SemanticAttributes.NET_PEER_PORT, 1234),
                                            equalTo(SemanticAttributes.HTTP_METHOD, "GET"),
                                            equalTo(SemanticAttributes.HTTP_FLAVOR, "2.0"),
                                            equalTo(SemanticAttributes.HTTP_STATUS_CODE, 200)))));

    listener.onEnd(context2, responseAttributes, nanos(300));

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("http.client.duration")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(point -> point.hasSum(300 /* millis */))),
            metric ->
                assertThat(metric)
                    .hasName("http.client.request.size")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(point -> point.hasSum(200 /* bytes */))),
            metric ->
                assertThat(metric)
                    .hasName("http.client.response.size")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(point -> point.hasSum(400 /* bytes */))));
  }

  private static long nanos(int millis) {
    return TimeUnit.MILLISECONDS.toNanos(millis);
  }
}
