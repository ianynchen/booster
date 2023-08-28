package io.github.booster.messaging.publisher.aws;

import arrow.core.Either;
import arrow.core.Option;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.factories.TaskFactory;
import io.github.booster.messaging.MessagingMetricsConstants;
import io.github.booster.messaging.config.AwsSqsConfig;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.publisher.MessagePublisher;
import io.github.booster.messaging.publisher.PublisherRecord;
import io.github.booster.messaging.util.MetricsHelper;
import io.github.booster.task.Task;
import io.micrometer.core.instrument.Timer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

/**
 * AWS SQS publisher
 * @param <T> type of payload
 */
public class AwsSqsPublisher<T> implements MessagePublisher<SqsRecord<T>> {

    private static final Logger log = LoggerFactory.getLogger(AwsSqsPublisher.class);

    /**
     * SQS publish time metric
     */
    public static final String SQS_PUBLISH_TIME = "sqs_publish_time";

    /**
     * SQS publish count metric
     */
    public static final String SQS_PUBLISH_COUNT = "sqs_publish_count";

    private final SqsClient sqsClient;

    private final String name;

    private final OpenTelemetryConfig openTelemetryConfig;

    private final ObjectMapper mapper;

    private final String queueUrl;

    private final Task<SqsRecord<T>, PublisherRecord> publisherTask;

    private final MetricsRegistry registry;

    private final boolean manuallyInjectTrace;

    /**
     * constructor
     * @param name name of publisher
     * @param awsSqsConfig {@link AwsSqsConfig} where {@link SqsClient} can be created from
     * @param openTelemetryConfig {@link OpenTelemetryConfig} for tracing
     * @param taskFactory {@link TaskFactory} to create publisher as a task with thread pools
     * @param mapper {@link ObjectMapper} for message serialization
     * @param registry {@link MetricsRegistry} to record metrics
     * @param manuallyInjectTrace whether to inject trace manually or rely on OTEl instrumentation
     */
    public AwsSqsPublisher(
            String name,
            AwsSqsConfig awsSqsConfig,
            OpenTelemetryConfig openTelemetryConfig,
            TaskFactory taskFactory,
            ObjectMapper mapper,
            MetricsRegistry registry,
            boolean manuallyInjectTrace
    ) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be empty");
        Preconditions.checkArgument(awsSqsConfig != null && awsSqsConfig.getClient(name).isDefined(), "sqs client has to be configured");
        Preconditions.checkArgument(taskFactory != null, "task factory cannot be null");
        Preconditions.checkArgument(registry != null, "metrics registry cannot be null");

        this.sqsClient = awsSqsConfig.getClient(name).orNull();
        this.name = name;
        this.openTelemetryConfig = openTelemetryConfig;
        this.manuallyInjectTrace = manuallyInjectTrace;
        this.mapper = mapper == null ? new ObjectMapper() : mapper;
        this.queueUrl = awsSqsConfig.get(name).getQueueUrl();
        this.registry = registry;

        this.publisherTask = taskFactory.getAsyncTask(
                name,
                this::publish
        );
    }

    private Mono<Option<PublisherRecord>> publish(SqsRecord<T> message) {
        Option<Timer.Sample> sample = this.registry.startSample();
        Either<Throwable, SendMessageRequest> either = message.createSendMessageRequest(
                this.queueUrl,
                this.mapper,
                this.openTelemetryConfig,
                this.manuallyInjectTrace
        );

        if (either.isRight()) {
            SendMessageRequest sendMessageRequest = either.getOrNull();

            try {
                SendMessageResponse response = this.sqsClient.sendMessage(sendMessageRequest);

                if (response != null && response.messageId() != null) {
                    log.info("booster-messaging - success sending to sqs: [{}], message: [{}]", this.queueUrl, message);
                    MetricsHelper.recordMessagePublishCount(
                            this.registry,
                            SQS_PUBLISH_COUNT,
                            MessagingMetricsConstants.AWS_SQS,
                            this.name,
                            this.queueUrl,
                            MessagingMetricsConstants.SUCCESS_STATUS,
                            MessagingMetricsConstants.SUCCESS_STATUS
                    );
                    return Mono.just(
                            Option.fromNullable(
                                    PublisherRecord.builder()
                                            .recordId(response.messageId())
                                            .topic(this.queueUrl)
                                            .build()
                            )
                    );
                } else {
                    log.warn("booster-messaging - failed to send to sqs: [{}] without exception", this.queueUrl);
                    MetricsHelper.recordMessagePublishCount(
                            this.registry,
                            SQS_PUBLISH_COUNT,
                            MessagingMetricsConstants.AWS_SQS,
                            this.name,
                            this.queueUrl,
                            MessagingMetricsConstants.FAILURE_STATUS,
                            MessagingMetricsConstants.FAILURE_STATUS
                    );
                    return Mono.error(new IllegalStateException("no response or no message ID from response"));
                }
            } catch (Throwable t) {
                log.error("booster-messaging - error publishing to sqs: [{}]", this.queueUrl, t);
                MetricsHelper.recordMessagePublishCount(
                        this.registry,
                        SQS_PUBLISH_COUNT,
                        MessagingMetricsConstants.AWS_SQS,
                        this.name,
                        this.queueUrl,
                        MessagingMetricsConstants.FAILURE_STATUS,
                        t.getClass().getSimpleName()
                );
                return Mono.error(t);
            } finally {
                MetricsHelper.recordProcessingTime(
                        this.registry,
                        sample,
                        SQS_PUBLISH_TIME,
                        MessagingMetricsConstants.AWS_SQS,
                        this.name
                );
            }
        } else {
            Throwable t = either.swap().getOrNull();
            log.error("booster-messaging - error converting message: [{}]", message, t);
            MetricsHelper.recordMessagePublishCount(
                    this.registry,
                    SQS_PUBLISH_COUNT,
                    MessagingMetricsConstants.AWS_SQS,
                    this.name,
                    this.queueUrl,
                    MessagingMetricsConstants.FAILURE_STATUS,
                    t.getClass().getSimpleName()
            );
            return Mono.error(t);
        }
    }

    /**
     * Publishes the message
     * @param topic topic to publish to
     * @param message the message to be published.
     * @return {@link Mono} of {@link Either} a {@link Throwable} or an {@link Option} of
     *         {@link PublisherRecord}
     */
    @Override
    public Mono<Either<Throwable, Option<PublisherRecord>>> publish(String topic, SqsRecord<T> message) {
        return this.publisherTask.execute(Option.fromNullable(message));
    }
}
