package io.github.booster.messaging.subscriber.aws;

import com.google.api.client.util.Preconditions;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.config.thread.ThreadPoolConfigGeneric;
import io.github.booster.messaging.MessagingMetricsConstants;
import io.github.booster.messaging.config.AwsSqsConfig;
import io.github.booster.messaging.config.AwsSqsSetting;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.subscriber.BatchSubscriberFlow;
import io.github.booster.messaging.subscriber.SubscriberFlow;
import io.github.booster.messaging.util.MetricsHelper;
import io.github.booster.messaging.util.TraceHelper;
import io.opentelemetry.context.propagation.TextMapGetter;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class AwsSqsSubscriber implements SubscriberFlow<Message>, BatchSubscriberFlow<Message> {

    public static class AwsSqsTextMapGetter implements TextMapGetter<Message> {

        @Override
        public Iterable<String> keys(Message carrier) {
            return carrier.messageAttributes().keySet();
        }

        @Nullable
        @Override
        public String get(@Nullable Message carrier, String key) {
            return carrier == null || !carrier.messageAttributes().containsKey(key) ? null :
                    carrier.messageAttributes().get(key).stringValue();
        }
    }

    private static final Logger log = LoggerFactory.getLogger(AwsSqsSubscriber.class);

    public static final AwsSqsTextMapGetter GETTER = new AwsSqsTextMapGetter();

    private final boolean manuallyInjectTrace;

    private volatile boolean stopped = false;

    private final String name;

    private final ExecutorService executorService;

    private final OpenTelemetryConfig openTelemetryConfig;

    private final SqsClient sqsClient;

    private final MetricsRegistry registry;

    private final AwsSqsSetting sqsSetting;

    public AwsSqsSubscriber(
            String name,
            AwsSqsConfig awsSqsConfig,
            ThreadPoolConfigGeneric threadPoolConfig,
            MetricsRegistry registry,
            OpenTelemetryConfig openTelemetryConfig,
            boolean manuallyInjectTrace
    ) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank");
        Preconditions.checkArgument(awsSqsConfig != null, "AwsSqsConfig cannot be null");
        Preconditions.checkArgument(
                awsSqsConfig.get(name) != null && awsSqsConfig.getClient(name).isDefined(),
                "SqsClient setting should be present and can be created"
        );
        Preconditions.checkArgument(threadPoolConfig != null, "thread pool config cannot be null");

        this.sqsSetting = awsSqsConfig.get(name);
        this.sqsClient = awsSqsConfig.getClient(name).orNull();
        this.name = name;
        this.registry = registry == null ? new MetricsRegistry() : registry;
        Preconditions.checkArgument(StringUtils.isNotBlank(this.sqsSetting.getQueueUrl()), "SQS queue URL cannot be blank");

        this.executorService = threadPoolConfig.get(this.name);
        Preconditions.checkArgument(this.executorService != null, "subscriber thread pool cannot be null");
        this.openTelemetryConfig = openTelemetryConfig;
        this.manuallyInjectTrace = manuallyInjectTrace;
    }

    private List<Message> pullRecords() {
        val sampleOption = this.registry.startSample();
        try {
            ReceiveMessageResponse response = this.sqsClient.receiveMessage(
                    this.sqsSetting
                            .getReceiverSetting()
                            .createRequest(this.sqsSetting.getQueueUrl())
            );
            List<Message> records = response.messages() == null ? List.of() : response.messages();

            if (records.size() > 0) {
                MetricsHelper.recordMessageSubscribeCount(
                        this.registry,
                        MessagingMetricsConstants.SUBSCRIBER_PULL_COUNT,
                        records.size(),
                        MessagingMetricsConstants.AWS_SQS,
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
                    MessagingMetricsConstants.AWS_SQS,
                    this.name
            );
        }
    }

    private Flux<List<Message>> generateFlux() {
        return Flux.generate(
                () -> this.stopped,
                (Boolean stopState, SynchronousSink<List<Message>> sink) -> {
                    List<Message> records = this.pullRecords();
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
        ).filter(entry -> entry != null && !CollectionUtils.isEmpty(entry));
    }

    public void stop() {
        this.stopped = true;
        this.executorService.shutdown();
    }

    @Override
    public Flux<List<Message>> flux() {
        return this.generateFlux()
                .flatMap(records -> {
                    if (this.manuallyInjectTrace) {
                        return TraceHelper.generateContextForList(this.openTelemetryConfig, records, GETTER);
                    }
                    return Mono.just(records);
                });
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Flux<Message> flatFlux() {
        return this.generateFlux()
                .flatMap(Flux::fromIterable)
                .flatMap(record -> {
                    if (this.manuallyInjectTrace) {
                        return TraceHelper.generateContextForItem(this.openTelemetryConfig, record, GETTER);
                    }
                    return Mono.just(record);
                });
    }

    public String getQueueUrl() {
        return this.sqsSetting.getQueueUrl();
    }
}
