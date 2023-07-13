package io.github.booster.messaging.subscriber.kafka;

import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * OpenTelemetry Instrumentation doesn't seem to be working for Kafka
 * producer and consumer. Adding this aspect to support it.
 *
 * Will require all method annotated with {@link org.springframework.kafka.annotation.KafkaListener} to
 * consume {@link ConsumerRecord} rather than the object embedded. This will allow
 * this aspect to extract trace information passed from producer.
 */
@Aspect
public class KafkaSubscriberAspect {

    public static final String TRACESTATE = "tracestate";
    public static final String BAGGAGE = "baggage";

    static class KafkaListenerTextMapGetter implements TextMapGetter<ConsumerRecord<?, ?>> {

        @Override
        public Iterable<String> keys(ConsumerRecord<?, ?> carrier) {
            return Stream.of(carrier.headers().toArray()).map(Header::key).collect(Collectors.toList());
        }

        @Nullable
        @Override
        public String get(@Nullable ConsumerRecord<?, ?> carrier, String key) {
            // somehow for kafka, it's looking for tracestate and baggage.
            if (key.equals(TRACESTATE)) {
                return "01";
            } else if (key.equals(BAGGAGE)) {
                return null;
            }
            return new String(carrier.headers().lastHeader(key).value(), StandardCharsets.UTF_8);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(KafkaSubscriberAspect.class);

    private final OpenTelemetryConfig openTelemetryConfig;

    private KafkaListenerTextMapGetter getter = new KafkaListenerTextMapGetter();

    public KafkaSubscriberAspect(OpenTelemetryConfig openTelemetryConfig) {
        this.openTelemetryConfig = openTelemetryConfig;
    }

    @Pointcut("execution(public * *(..)) && @annotation(org.springframework.kafka.annotation.KafkaListener)")
    public void anyPublicListener() {

    }

    @Around("anyPublicListener()")
    public void kafkaListener(ProceedingJoinPoint joinPoint) {

        ConsumerRecord record = null;

        for (Object a: joinPoint.getArgs()) {
            if (a instanceof ConsumerRecord) {
                record = (ConsumerRecord) a;
                break;
            }
        }

        if (record != null) {
            if (this.openTelemetryConfig != null) {
                Context populatedContext = this.openTelemetryConfig.getOpenTelemetry()
                        .getPropagators()
                        .getTextMapPropagator()
                        .extract(Context.current(), record, getter);
                try (Scope scope = populatedContext.makeCurrent()) {
                    Span span = this.openTelemetryConfig.getTracer()
                            .spanBuilder("kafka-consumer")
                            .setSpanKind(SpanKind.CONSUMER)
                            .startSpan();
                    try {
                        joinPoint.proceed(joinPoint.getArgs());
                    } catch (Throwable t) {
                        span.setStatus(StatusCode.ERROR, t.getLocalizedMessage());
                    } finally {
                        span.end();
                    }
                }
            } else {
                try {
                    joinPoint.proceed(joinPoint.getArgs());
                } catch (Throwable t) {
                    log.error("booster-messaging - kafka subscriber aspect unable to proceed from consumer aspect", t);
                }
            }
        } else {
            try {
                joinPoint.proceed(joinPoint.getArgs());
            } catch (Throwable t) {
                log.error("booster-messaging - kafka subscriber aspect unable to proceed from consumer aspect", t);
            }
        }
    }
}
