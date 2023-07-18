package io.github.booster.messaging.util;

import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.subscriber.aws.AwsSqsSubscriber;
import io.github.booster.messaging.subscriber.gcp.GcpPubSubPullSubscriber;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public interface TraceHelper {

    ContextKey<SpanContext> CONSUMER_CONTEXT_KEY =
            ContextKey.named("consumer_context");

    Logger log = LoggerFactory.getLogger(TraceHelper.class);

    static Context createContext(
            OpenTelemetryConfig openTelemetryConfig,
            List<AcknowledgeablePubsubMessage> records,
            GcpPubSubPullSubscriber.GcpPubSubTextMapGetter getter
    ) {
        if (CollectionUtils.isEmpty(records) || openTelemetryConfig == null || openTelemetryConfig.getOpenTelemetry() == null) {
            return Context.current();
        }
        AcknowledgeablePubsubMessage last = records.get(records.size() - 1);
        return openTelemetryConfig.getOpenTelemetry()
                .getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), last.getPubsubMessage(), getter);
    }

    static Context createContext(
            OpenTelemetryConfig openTelemetryConfig,
            List<Message> records,
            AwsSqsSubscriber.AwsSqsTextMapGetter getter
    ) {
        if (CollectionUtils.isEmpty(records) || openTelemetryConfig == null || openTelemetryConfig.getOpenTelemetry() == null) {
            return Context.current();
        }
        Message last = records.get(records.size() - 1);
        return openTelemetryConfig.getOpenTelemetry()
                .getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), last, getter);
    }

    static Mono<List<AcknowledgeablePubsubMessage>> generateContextForList(
            OpenTelemetryConfig openTelemetryConfig,
            List<AcknowledgeablePubsubMessage> records,
            GcpPubSubPullSubscriber.GcpPubSubTextMapGetter getter
    ) {
        Context context = TraceHelper.createContext(openTelemetryConfig, records, getter);

        assert context != null;
        if (openTelemetryConfig != null) {
            Span childSpan = openTelemetryConfig
                    .getTracer()
                    .spanBuilder("gcp-pub-sub-consumer")
                    .setParent(context)
                    .setSpanKind(SpanKind.CONSUMER)
                    .startSpan();

            try (Scope childScope = childSpan.makeCurrent()) {
                log.debug("booster-messaging - generating span for records: [{}]", records);
                return Mono.just(records)
                        .contextWrite(ctx -> ctx.put(CONSUMER_CONTEXT_KEY, childSpan.getSpanContext()));
            } finally {
                childSpan.end();
            }
        } else {
            return Mono.just(records);
        }
    }

    static Mono<AcknowledgeablePubsubMessage> generateContextForItem(
            OpenTelemetryConfig openTelemetryConfig,
            AcknowledgeablePubsubMessage record,
            GcpPubSubPullSubscriber.GcpPubSubTextMapGetter getter
    ) {
        assert record != null;
        Context context = TraceHelper.createContext(openTelemetryConfig, List.of(record), getter);

        assert context != null;
        if (openTelemetryConfig != null) {
            Span childSpan = openTelemetryConfig
                    .getTracer()
                    .spanBuilder("gcp-pub-sub-consumer")
                    .setParent(context)
                    .setSpanKind(SpanKind.CONSUMER)
                    .startSpan();

            try (Scope childScope = childSpan.makeCurrent()) {
                log.debug("booster-messaging - generating span for record: [{}]", record);
                return Mono.just(record)
                        .map(message -> {
                            Context ctx = Context.current()
                                    .with(CONSUMER_CONTEXT_KEY, childSpan.getSpanContext());
                            return message;
                        }).contextWrite(ctx -> ctx.put(CONSUMER_CONTEXT_KEY, childSpan.getSpanContext()))
                        .doOnTerminate(childSpan::end);
            } finally {
                childSpan.end();
            }
        } else {
            return Mono.just(record);
        }
    }

    static Mono<List<Message>> generateContextForList(
            OpenTelemetryConfig openTelemetryConfig,
            List<Message> records,
            AwsSqsSubscriber.AwsSqsTextMapGetter getter
    ) {
        Context context = TraceHelper.createContext(openTelemetryConfig, records, getter);

        assert context != null;
        if (openTelemetryConfig != null) {
            Span childSpan = openTelemetryConfig
                    .getTracer()
                    .spanBuilder("gcp-pub-sub-consumer")
                    .setParent(context)
                    .setSpanKind(SpanKind.CONSUMER)
                    .startSpan();

            try (Scope childScope = childSpan.makeCurrent()) {
                log.debug("booster-messaging - generating span for records: [{}]", records);
                return Mono.just(records)
                        .contextWrite(ctx -> ctx.put(CONSUMER_CONTEXT_KEY, childSpan.getSpanContext()));
            } finally {
                childSpan.end();
            }
        } else {
            return Mono.just(records);
        }
    }

    static Mono<Message> generateContextForItem(
            OpenTelemetryConfig openTelemetryConfig,
            Message record,
            AwsSqsSubscriber.AwsSqsTextMapGetter getter
    ) {
        assert record != null;
        Context context = TraceHelper.createContext(openTelemetryConfig, List.of(record), getter);

        assert context != null;
        if (openTelemetryConfig != null) {
            Span childSpan = openTelemetryConfig
                    .getTracer()
                    .spanBuilder("gcp-pub-sub-consumer")
                    .setParent(context)
                    .setSpanKind(SpanKind.CONSUMER)
                    .startSpan();

            try (Scope childScope = childSpan.makeCurrent()) {
                log.debug("booster-messaging - generating span for record: [{}]", record);
                return Mono.just(record)
                        .map(message -> {
                            Context ctx = Context.current()
                                    .with(CONSUMER_CONTEXT_KEY, childSpan.getSpanContext());
                            return message;
                        }).contextWrite(ctx -> ctx.put(CONSUMER_CONTEXT_KEY, childSpan.getSpanContext()))
                        .doOnTerminate(childSpan::end);
            } finally {
                childSpan.end();
            }
        } else {
            return Mono.just(record);
        }
    }

    static void releaseSpanAndScope(
            AtomicReference<Scope> scopeReference,
            AtomicReference<Span> spanReference
    ) {
        log.debug("booster-messaging - close span and scope");
        Span oldSpan = spanReference.getAndSet(null);
        if (oldSpan != null) {
            oldSpan.end();
            log.debug("booster-messaging - span [{}] ended", oldSpan);
        }
        Scope oldScope = scopeReference.getAndSet(null);
        if (oldScope != null) {
            oldScope.close();
            log.debug("booster-messaging - scope [{}] closed", oldScope);
        }
    }

    static Context createContext(
            OpenTelemetryConfig openTelemetryConfig,
            AcknowledgeablePubsubMessage record
    ) {
        if (record == null || openTelemetryConfig == null || openTelemetryConfig.getOpenTelemetry() == null) {
            return Context.current();
        }
        return openTelemetryConfig.getOpenTelemetry()
                .getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), record.getPubsubMessage(), GcpPubSubPullSubscriber.GETTER);
    }

    static Span createSpan(
            OpenTelemetryConfig openTelemetryConfig,
            Context context
    ) {
        if (openTelemetryConfig != null && context != null) {
            return openTelemetryConfig
                    .getTracer()
                    .spanBuilder("gcp-pub-sub-consumer")
                    .setParent(context)
                    .setSpanKind(SpanKind.CONSUMER)
                    .startSpan();
        }
        return Span.current();
    }
}
