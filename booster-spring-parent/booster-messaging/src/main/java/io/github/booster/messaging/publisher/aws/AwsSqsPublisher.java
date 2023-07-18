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

public class AwsSqsPublisher<T> implements MessagePublisher<AwsMessage<T>> {

    private static final Logger log = LoggerFactory.getLogger(AwsSqsPublisher.class);
    public static final String SQS_PUBLISH_TIME = "sqs_publish_time";
    public static final String SQS_PUBLISH_COUNT = "sqs_publish_count";

    private final SqsClient sqsClient;

    private final String name;

    private final OpenTelemetryConfig openTelemetryConfig;

    private final ObjectMapper mapper;

    private final String queryUrl;

    private final Task<AwsMessage<T>, PublisherRecord> publisherTask;

    private final MetricsRegistry registry;

    private final boolean manuallyInjectTrace;

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
        this.mapper = mapper;
        this.queryUrl = awsSqsConfig.get(name).getUrl();
        this.registry = registry;

        this.publisherTask = taskFactory.getAsyncTask(
                name,
                this::publish
        );
    }

    private Mono<PublisherRecord> publish(AwsMessage<T> message) {
        Option<Timer.Sample> sample = this.registry.startSample();
        Either<Throwable, SendMessageRequest> either = message.createRequest(
                this.queryUrl,
                this.mapper,
                this.openTelemetryConfig,
                this.manuallyInjectTrace
        );

        if (either.isRight()) {
            SendMessageRequest sendMessageRequest = either.getOrNull();

            try {
                SendMessageResponse response = this.sqsClient.sendMessage(sendMessageRequest);

                if (response != null && response.messageId() != null) {
                    log.info("booster-messaging - success sending to sqs: [{}], message: [{}]", this.queryUrl, message);
                    MetricsHelper.recordMessagePublishCount(
                            this.registry,
                            SQS_PUBLISH_COUNT,
                            MessagingMetricsConstants.AWS_SQS,
                            this.name,
                            this.queryUrl,
                            MessagingMetricsConstants.SUCCESS_STATUS,
                            MessagingMetricsConstants.SUCCESS_STATUS
                    );
                } else {
                    log.warn("booster-messaging - failed to send to sqs: [{}] without exception", this.queryUrl);
                    MetricsHelper.recordMessagePublishCount(
                            this.registry,
                            SQS_PUBLISH_COUNT,
                            MessagingMetricsConstants.AWS_SQS,
                            this.name,
                            this.queryUrl,
                            MessagingMetricsConstants.FAILURE_STATUS,
                            MessagingMetricsConstants.FAILURE_STATUS
                    );
                }
                return Mono.just(
                        PublisherRecord.builder()
                                .recordId(response.messageId())
                                .topic(this.queryUrl)
                                .build()
                );
            } catch (Throwable t) {
                log.error("booster-messaging - error publishing to sqs: [{}]", this.queryUrl, t);
                MetricsHelper.recordMessagePublishCount(
                        this.registry,
                        SQS_PUBLISH_COUNT,
                        MessagingMetricsConstants.AWS_SQS,
                        this.name,
                        this.queryUrl,
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
                    this.queryUrl,
                    MessagingMetricsConstants.FAILURE_STATUS,
                    t.getClass().getSimpleName()
            );
            return Mono.error(t);
        }
    }

    @Override
    public Mono<Either<Throwable, PublisherRecord>> publish(String topic, AwsMessage<T> message) {
        return this.publisherTask.execute(message);
    }
}
