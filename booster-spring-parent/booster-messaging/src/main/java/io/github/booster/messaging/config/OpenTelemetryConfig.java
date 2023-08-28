package io.github.booster.messaging.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.extension.trace.propagation.OtTracePropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

/**
 * OpenTelemetry configuration
 */
public class OpenTelemetryConfig {

    private static final String INSTRUMENTATION_NAME = "booster-messaging";
    private static final String INSTRUMENTATION_VERSION = "1.23.1";

    private final OpenTelemetry openTelemetry;

    /**
     * Constructor to create an {@link OpenTelemetryConfig}
     * @param openTelemetry an {@link OpenTelemetry} object. If this is not null,
     *                      the instance will be used to generate and get trace info. Otherwise
     *                      a new instance will be created and used.
     * @param serviceName name of the service
     */
    public OpenTelemetryConfig(
            OpenTelemetry openTelemetry,
            String serviceName
    ) {
        if (openTelemetry == null) {
            Resource resource = Resource.getDefault()
                    .merge(
                            Resource.create(
                                    Attributes.of(
                                            ResourceAttributes.SERVICE_NAME,
                                            serviceName
                                    )
                            )
                    );

            SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                    .setResource(resource)
                    .build();

            SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder()
                    .registerMetricReader(PeriodicMetricReader.builder(OtlpGrpcMetricExporter.builder().build()).build())
                    .registerMetricReader(PeriodicMetricReader.builder(OtlpHttpMetricExporter.builder().build()).build())
                    .setResource(resource)
                    .build();

            ContextPropagators contextPropagators = ContextPropagators.create(
                    TextMapPropagator.composite(
                            W3CTraceContextPropagator.getInstance(),
                            B3Propagator.injectingSingleHeader(),
                            B3Propagator.injectingMultiHeaders(),
                            JaegerPropagator.getInstance(),
                            OtTracePropagator.getInstance()
                    )
            );

            // do not register as global as it will throw illegal state exception
            this.openTelemetry = OpenTelemetrySdk.builder()
                    .setTracerProvider(sdkTracerProvider)
                    .setMeterProvider(sdkMeterProvider)
                    .setPropagators(contextPropagators)
                    .build();
        } else {
            this.openTelemetry = openTelemetry;
        }
    }

    /**
     * Gets {@link Tracer}
     * @return {@link Tracer}
     */
    public Tracer getTracer() {
        return this.openTelemetry
                .getTracerProvider()
                .get(INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);
    }

    /**
     * Gets {@link OpenTelemetry}
     * @return {@link OpenTelemetry}
     */
    public OpenTelemetry getOpenTelemetry() {
        return this.openTelemetry;
    }
}
