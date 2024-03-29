package io.github.booster.messaging.publisher.gcp;

import arrow.core.Either;
import arrow.core.Option;
import com.google.api.client.util.Preconditions;
import com.google.cloud.spring.pubsub.core.publisher.PubSubPublisherTemplate;
import com.google.pubsub.v1.PubsubMessage;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.util.EitherUtil;
import io.github.booster.config.thread.ThreadPoolConfig;
import io.github.booster.messaging.MessagingMetricsConstants;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.publisher.PublisherRecord;
import io.github.booster.messaging.publisher.MessagePublisher;
import io.github.booster.messaging.util.FutureHelper;
import io.github.booster.messaging.util.MetricsHelper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * GCP pub/sub publisher
 * @param <T> type of actual payload to be sent.
 */
public class GcpPubSubPublisher<T> implements MessagePublisher<PubsubRecord<T>> {

    /**
     * {@link TextMapSetter} for GCP pub/sub messages
     */
    protected static class GcpPubSubTextMapSetter implements TextMapSetter<Map<String, String>> {

        /**
         * Default constructor
         */
        public GcpPubSubTextMapSetter() {
            super();
        }

        /**
         * Sets tracing fields.
         * @param carrier holds propagation fields. For example, an outgoing message or http request. To
         *     facilitate implementations as java lambdas, this parameter may be null.
         * @param key the key of the field.
         * @param value the value of the field.
         */
        @Override
        public void set(@Nullable Map<String, String> carrier, String key, String value) {
            if (carrier != null) {
                carrier.put(key, value);
            }
        }
    }

    private static final Logger log = LoggerFactory.getLogger(GcpPubSubPublisher.class);

    private static final GcpPubSubTextMapSetter textMapSetter = new GcpPubSubTextMapSetter();

    private final PubSubPublisherTemplate template;

    private final String name;

    private final MetricsRegistry registry;

    private final ExecutorService executorService;

    private final OpenTelemetryConfig openTelemetryConfig;

    private final boolean manuallyInjectTrace;

    /**
     * Constructs a GCP pub/sub subscriber. Name
     * of the subscriber is also used to retrieve thread pools from
     * {@link ThreadPoolConfig} to run {@link PubSubPublisherTemplate#publish(String, PubsubMessage)} on.
     * @param name name of subscriber and name of thread pool to use
     * @param template {@link PubSubPublisherTemplate} to publish
     * @param threadPoolConfig {@link ThreadPoolConfig} to create threads to run publish on.
     * @param registry to record metrics.
     * @param openTelemetryConfig {@link OpenTelemetryConfig} for tracing
     * @param manuallyInjectTrace whether to manually inject trace or leave
     *                            to OTEL instrumentation.
     */
    public GcpPubSubPublisher(
            String name,
            PubSubPublisherTemplate template,
            ThreadPoolConfig threadPoolConfig,
            MetricsRegistry registry,
            OpenTelemetryConfig openTelemetryConfig,
            boolean manuallyInjectTrace
    ) {
        Preconditions.checkArgument(template != null, "PubSubPublisherTemplate cannot be null");
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank");
        Preconditions.checkArgument(threadPoolConfig != null, "thread pool config cannot be null");
        Preconditions.checkArgument(
                threadPoolConfig.get(name) != null, "executor service cannot be null");

        this.template = template;
        this.name = name;
        this.executorService = threadPoolConfig.get(name);
        this.registry = registry == null ? new MetricsRegistry() : registry;
        this.openTelemetryConfig = openTelemetryConfig;
        this.manuallyInjectTrace = manuallyInjectTrace;
    }

    /**
     * Publishes to pub/sub
     * @param topic topic to publish to
     * @param message the message to be published.
     * @return {@link PublisherRecord} when successful, or {@link Throwable} in case of exceptions.
     */
    @Override
    public Mono<Either<Throwable, Option<PublisherRecord>>> publish(String topic, PubsubRecord<T> message) {

        val sample = this.registry.startSample();
        PubsubMessage pubsubMessage = this.createMessage(message);
        log.debug("booster-messaging - gcp publisher[{}] publishing to topic: {}, payload: {}", this.name, topic, pubsubMessage);

        Mono<String> publishMono = FutureHelper.fromListenableFutureToMono(this.template.publish(topic, pubsubMessage))
                .subscribeOn(Schedulers.fromExecutorService(this.executorService));

        return publishMono.map(string -> {
                    log.debug("booster-messaging - gcp publisher[{}] send successful", this.name);
                    MetricsHelper.recordMessagePublishCount(
                            this.registry,
                            MessagingMetricsConstants.SEND_COUNT,
                            MessagingMetricsConstants.GCP_PUBSUB,
                            this.name,
                            topic,
                            MessagingMetricsConstants.SUCCESS_STATUS,
                            MessagingMetricsConstants.SUCCESS_STATUS
                    );
                    return EitherUtil.convertData(
                            Option.fromNullable(
                                    PublisherRecord.builder().topic(topic).recordId(string).build()
                            )
                    );
                }).onErrorResume(throwable -> {
                    log.error("booster-messaging - gcp publisher[{}] send failed", this.name, throwable);
                    MetricsHelper.recordMessagePublishCount(
                            this.registry,
                            MessagingMetricsConstants.SEND_COUNT,
                            MessagingMetricsConstants.GCP_PUBSUB,
                            this.name,
                            topic,
                            MessagingMetricsConstants.FAILURE_STATUS,
                            throwable.getClass().getSimpleName()
                    );
                    return Mono.just(EitherUtil.convertThrowable(throwable));
                }).doOnTerminate(() -> {
                    log.debug("booster-messaging - gcp publisher[{}] message send terminated", this.name);
                    MetricsHelper.recordProcessingTime(
                            this.registry,
                            sample,
                            MessagingMetricsConstants.SEND_TIME,
                            MessagingMetricsConstants.GCP_PUBSUB,
                            this.name
                    );
                });
    }

    private PubsubMessage createMessage(PubsubRecord<T> message) {

        Map<String, String> newHeaders = new HashMap<>(message.getHeaders());

        if (this.manuallyInjectTrace && this.openTelemetryConfig != null) {
            Span currentSpan = Span.current();
            try (Scope scope = currentSpan.makeCurrent()) {
                this.openTelemetryConfig.getOpenTelemetry()
                        .getPropagators()
                        .getTextMapPropagator()
                        .inject(Context.current(), newHeaders, textMapSetter);
            } finally {
                currentSpan.end();
            }
        }

        return this.template.getMessageConverter()
                .toPubSubMessage(
                        message.getPayload(),
                        newHeaders
                );
    }
}
