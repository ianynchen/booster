package io.github.booster.messaging.subscriber.gcp;

import com.google.api.client.util.Preconditions;
import com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberTemplate;
import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;
import com.google.pubsub.v1.PubsubMessage;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.config.thread.ThreadPoolConfig;
import io.github.booster.messaging.MessagingMetricsConstants;
import io.github.booster.messaging.config.GcpPubSubSubscriberConfig;
import io.github.booster.messaging.config.GcpPubSubSubscriberSetting;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.subscriber.BatchSubscriberFlow;
import io.github.booster.messaging.subscriber.SubscriberFlow;
import io.github.booster.messaging.util.MetricsHelper;
import io.github.booster.messaging.util.TraceHelper;
import io.opentelemetry.context.propagation.TextMapGetter;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

/**
 * Pull based GCP pub/sub subscriber. Internally uses
 * a pull based mechanism to pull messages from GCP.
 */
public class GcpPubSubPullSubscriber
        implements SubscriberFlow<AcknowledgeablePubsubMessage>, BatchSubscriberFlow<AcknowledgeablePubsubMessage> {

    /**
     * {@link TextMapGetter} to get tracing from GCP pub/sub messages
     */
    public static class GcpPubSubTextMapGetter implements TextMapGetter<PubsubMessage> {

        /**
         * Default constructor
         */
        public GcpPubSubTextMapGetter() {
            super();
        }

        /**
         * Get all keys for message attributes
         * @param carrier carrier of propagation fields, such as an http request.
         * @return Get all keys for message attributes
         */
        @Override
        public Iterable<String> keys(PubsubMessage carrier) {
            return carrier.getAttributesMap().keySet();
        }

        /**
         * Get tracing related field values from attribute map
         * @param carrier carrier of propagation fields, such as an http request.
         * @param key the key of the field.
         * @return value associated with the key
         */
        @Nullable
        @Override
        public String get(@Nullable PubsubMessage carrier, String key) {
            return carrier == null ? null :
                    carrier.getAttributesMap().get(key);
        }
    }

    private static final Logger log = LoggerFactory.getLogger(GcpPubSubPullSubscriber.class);

    /**
     * Gets trace
     */
    public static final GcpPubSubTextMapGetter GETTER = new GcpPubSubTextMapGetter();

    private final PubSubSubscriberTemplate subscriberTemplate;

    private final String name;

    private final MetricsRegistry registry;

    private final GcpPubSubSubscriberSetting gcpPubSubSubscriberSetting;

    private volatile boolean stopped;

    private final ExecutorService executorService;

    private final OpenTelemetryConfig openTelemetryConfig;

    private final boolean manuallyInjectTrace;

    /**
     * Constructs a {@link GcpPubSubPullSubscriber}
     * @param name name of the {@link GcpPubSubPullSubscriber}. Name is
     *             also used to get thread pools and subscriber configuration.
     * @param subscriberTemplate {@link PubSubSubscriberTemplate} to perform subscriber operations
     * @param threadPoolConfig {@link ThreadPoolConfig} for subscriber
     * @param gcpPubSubSubscriberConfig {@link GcpPubSubSubscriberConfig} configuration settings
     * @param registry {@link MetricsRegistry} for metrics reporting
     * @param openTelemetryConfig {@link OpenTelemetryConfig} for tracing
     * @param manuallyInjectTrace whether to manually record traces or rely on OTEL instrumentation
     */
    public GcpPubSubPullSubscriber(
            String name,
            PubSubSubscriberTemplate subscriberTemplate,
            ThreadPoolConfig threadPoolConfig,
            GcpPubSubSubscriberConfig gcpPubSubSubscriberConfig,
            MetricsRegistry registry,
            OpenTelemetryConfig openTelemetryConfig,
            boolean manuallyInjectTrace
    ) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank");
        Preconditions.checkArgument(subscriberTemplate != null, "subscriber template cannot be null");
        Preconditions.checkArgument(threadPoolConfig != null, "thread pool config cannot be null");
        Preconditions.checkArgument(gcpPubSubSubscriberConfig != null, "gcp pubsub subscriber config cannot be null");

        this.subscriberTemplate = subscriberTemplate;
        this.name = name;
        this.registry = registry == null ? new MetricsRegistry() : registry;
        this.gcpPubSubSubscriberSetting = gcpPubSubSubscriberConfig.getSetting(name);
        Preconditions.checkArgument(this.gcpPubSubSubscriberSetting != null, "gcp pubsub subscriber setting cannot be null");

        this.executorService = threadPoolConfig.get(this.name);
        Preconditions.checkArgument(this.executorService != null, "subscriber thread pool cannot be null");
        this.openTelemetryConfig = openTelemetryConfig;
        this.manuallyInjectTrace = manuallyInjectTrace;
    }

    private List<AcknowledgeablePubsubMessage> pullRecords() {
        val sampleOption = this.registry.startSample();
        try {
            List<AcknowledgeablePubsubMessage> records = this.subscriberTemplate.pull(
                    this.gcpPubSubSubscriberSetting.getSubscription(),
                    this.gcpPubSubSubscriberSetting.getMaxRecords(),
                    true
            );

            if (CollectionUtils.isNotEmpty(records)) {
                MetricsHelper.recordMessageSubscribeCount(
                        this.registry,
                        MessagingMetricsConstants.SUBSCRIBER_PULL_COUNT,
                        records.size(),
                        MessagingMetricsConstants.GCP_PUBSUB,
                        this.name,
                        MessagingMetricsConstants.SUCCESS_STATUS,
                        MessagingMetricsConstants.SUCCESS_STATUS
                );
            }

            return records;
        } catch (Throwable t) {
            log.error("booster messaging - error pulling pub/sub messages in subscriber[{}]", this.name, t);
            MetricsHelper.recordMessageSubscribeCount(
                    this.registry,
                    MessagingMetricsConstants.SUBSCRIBER_PULL_COUNT,
                    MessagingMetricsConstants.GCP_PUBSUB,
                    this.name,
                    MessagingMetricsConstants.FAILURE_STATUS,
                    t.getClass().getSimpleName()
            );
            return List.of();
        } finally {
            MetricsHelper.recordProcessingTime(
                    this.registry,
                    sampleOption,
                    MessagingMetricsConstants.SUBSCRIBER_PULL_TIME,
                    MessagingMetricsConstants.GCP_PUBSUB,
                    this.name
            );
        }
    }

    /**
     * Stops pulling from pub/sub.
     */
    public void stop() {
        this.stopped = true;
        this.executorService.shutdown();
    }

    private Flux<List<AcknowledgeablePubsubMessage>> generateFlux() {
        return Flux.generate(
                () -> this.stopped,
                (Boolean stopState, SynchronousSink<List<AcknowledgeablePubsubMessage>> sink) -> {
                    List<AcknowledgeablePubsubMessage> records = this.pullRecords();
                    sink.next(records);
                    if (this.stopped) {
                        sink.complete();
                    }
                    return this.stopped;
                },
                stopState -> {
                    if (this.executorService != null) {
                        log.info("booster-messaging - shutting down thread pool for subscriber[{}]", this.name);
                        this.executorService.shutdown();
                    }
                    log.info("booster-messaging - queue[{}] stopped", this.name);
                }
        ).publishOn(Schedulers.fromExecutorService(this.executorService))
        .filter(entry -> entry != null && !CollectionUtils.isEmpty(entry));
    }

    /**
     * Creates a flux from messages in the {@link BlockingQueue}.
     * @return a {@link Flux} of non-empty {@link List} of {@link AcknowledgeablePubsubMessage} messages.
     */
    @Override
    public Flux<List<AcknowledgeablePubsubMessage>> flux() {
        return this.generateFlux()
                .flatMap(records -> {
                    if (this.manuallyInjectTrace) {
                        return TraceHelper.generateContextForList(this.openTelemetryConfig, records, GETTER);
                    }
                    return Mono.just(records);
                });
    }

    /**
     * Flattens the output of the messages.
     * @return a {@link Flux} of non-null {@link AcknowledgeablePubsubMessage}
     */
    @Override
    public Flux<AcknowledgeablePubsubMessage> flatFlux() {
        return this.generateFlux()
                .flatMap(Flux::fromIterable)
                .flatMap(record -> {
                    if (this.manuallyInjectTrace) {
                        return TraceHelper.generateContextForItem(this.openTelemetryConfig, record, GETTER);
                    }
                    return Mono.just(record);
                });
    }

    /**
     * Name of the subscriber
     * @return name of the subscriber
     */
    @Override
    public String getName() {
        return this.name;
    }
}
