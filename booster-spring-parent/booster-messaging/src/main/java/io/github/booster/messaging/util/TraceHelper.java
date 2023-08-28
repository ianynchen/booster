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

/**
 * Helper methods to inject trace
 */
public interface TraceHelper {

    /**
     * {@link ContextKey}
     */
    ContextKey<SpanContext> CONSUMER_CONTEXT_KEY =
            ContextKey.named("consumer_context");

    /**
     * {@link Logger} for logs
     */
    Logger log = LoggerFactory.getLogger(TraceHelper.class);

    /**
     * Creates a {@link Context} on pub/sub consumer side from records received
     * @param openTelemetryConfig {@link OpenTelemetryConfig}
     * @param records records to retrieve the context from
     * @param getter {@link GcpPubSubPullSubscriber.GcpPubSubTextMapGetter} to extract trace
     * @return {@link Context}
     */
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

    /**
     * Creates a {@link Context} on SQS consumer side from records received
     * @param openTelemetryConfig {@link OpenTelemetryConfig}
     * @param records records to retrieve the context from
     * @param getter {@link AwsSqsSubscriber.AwsSqsTextMapGetter} to extract trace
     * @return {@link Context}
     */
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

    /**
     * Generate {@link Context} from a list of GCP pub/sub message, the
     * trace is extracted from the last message in the list, all
     * previous trace info will be lost.
     * @param openTelemetryConfig {@link OpenTelemetryConfig}
     * @param records GCP pub/sub records
     * @param getter {@link GcpPubSubPullSubscriber.GcpPubSubTextMapGetter}
     * @return GCP pub/sub messages with last trace extracted
     */
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

    /**
     * Generate {@link Context} from a single GCP pub/sub message
     * @param openTelemetryConfig {@link OpenTelemetryConfig}
     * @param record GCP pub/sub record
     * @param getter {@link GcpPubSubPullSubscriber.GcpPubSubTextMapGetter}
     * @return GCP pub/sub message with trace extracted
     */
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

    /**
     * Generate {@link Context} from a list of AWS SQS message, the
     * trace is extracted from the last message in the list, all
     * previous trace info will be lost.
     * @param openTelemetryConfig {@link OpenTelemetryConfig}
     * @param records AWS SQS records
     * @param getter {@link AwsSqsSubscriber.AwsSqsTextMapGetter}
     * @return SQS messages with last trace extracted
     */
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

    /**
     * Generate {@link Context} from a single AWS SQS message
     * @param openTelemetryConfig {@link OpenTelemetryConfig}
     * @param record AWS SQS record
     * @param getter {@link AwsSqsSubscriber.AwsSqsTextMapGetter}
     * @return SQS message with trace extracted
     */
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

    /**
     * Releases {@link Span} and {@link Scope}
     * @param scopeReference {@link AtomicReference} of {@link Scope}
     * @param spanReference {@link AtomicReference} of {@link Span}
     */
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

    /**
     * Creates {@link Context} from a pub/sub message
     * @param openTelemetryConfig {@link OpenTelemetryConfig}
     * @param record GCP pub/sub message
     * @return a new {@link Context} or current context
     */
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

    /**
     * Creates {@link Span} from {@link Context}
     * @param openTelemetryConfig {@link OpenTelemetryConfig}
     * @param context {@link Context}
     * @return new {@link Span} from {@link Context} or current span
     */
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
