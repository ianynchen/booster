package io.github.booster.messaging.processor.aws;

import com.google.api.client.util.Preconditions;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.messaging.MessagingMetricsConstants;
import io.github.booster.messaging.config.AwsSqsConfig;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.processor.AbstractProcessor;
import io.github.booster.messaging.subscriber.SubscriberFlow;
import io.github.booster.messaging.subscriber.aws.AwsSqsSubscriber;
import io.github.booster.messaging.util.TraceHelper;
import io.github.booster.task.Task;
import io.opentelemetry.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

/**
 * Processor for AWS SQS messages
 */
public class AwsSqsProcessor extends AbstractProcessor<Message> {

    private static final Logger log = LoggerFactory.getLogger(AwsSqsProcessor.class);

    private final SqsClient sqsClient;

    private final String queueUrl;

    /**
     * Constructor
     *
     * @param subscriberFlow      the {@link SubscriberFlow} to listen to
     * @param awsSqsConfig        {@link AwsSqsConfig} where {@link SqsClient} is created
     * @param processTask         processor {@link Task} to process events coming from {@link SubscriberFlow}
     * @param openTelemetryConfig {@link OpenTelemetryConfig} for tracing
     * @param registry            metrics recording.
     * @param manuallyInjectTrace whether to inject tract manually or use OTEL instrumentation
     */
    public AwsSqsProcessor(
            SubscriberFlow<Message> subscriberFlow,
            AwsSqsConfig awsSqsConfig,
            Task<Message, Message> processTask,
            OpenTelemetryConfig openTelemetryConfig,
            MetricsRegistry registry,
            boolean manuallyInjectTrace
    ) {
        super(
                MessagingMetricsConstants.AWS_SQS,
                subscriberFlow,
                processTask,
                openTelemetryConfig,
                registry,
                manuallyInjectTrace
        );

        Preconditions.checkArgument(
                awsSqsConfig != null && awsSqsConfig.getClient(subscriberFlow.getName()).isDefined(),
                "AWS SQS config must not be null and SQS configuration setting must exist for the client"
        );
        this.sqsClient = awsSqsConfig.getClient(subscriberFlow.getName()).orNull();
        this.queueUrl = awsSqsConfig.get(subscriberFlow.getName()).getQueueUrl();
    }

    @Override
    protected boolean acknowledge(Message record) {
        DeleteMessageRequest request = DeleteMessageRequest.builder()
                .queueUrl(this.queueUrl)
                .receiptHandle(record.receiptHandle())
                .build();

        try {
            DeleteMessageResponse response = sqsClient.deleteMessage(request);

            if (response != null && response.sdkHttpResponse().isSuccessful()) {
                log.info("booster-messaging - deleted sqs message: [{}]", record.messageId());
                return true;
            }
            log.warn("booster-messaging - unable to delete sqs message: [{}], no exceptions", record.messageId());
            return false;
        } catch (Throwable t) {
            log.error("booster-messaging - unable to delete sqs message: [{}], with error", record.messageId(), t);
            return false;
        }
    }

    @Override
    protected Context createContext(Message record) {
        return TraceHelper.createContext(
                this.openTelemetryConfig,
                List.of(record),
                AwsSqsSubscriber.GETTER
        );
    }
}
