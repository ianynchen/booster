package io.github.booster.messaging.processor.aws;

import com.google.api.client.util.Preconditions;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.messaging.MessagingMetricsConstants;
import io.github.booster.messaging.config.AwsSqsConfig;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.processor.AbstractBatchProcessor;
import io.github.booster.messaging.subscriber.BatchSubscriberFlow;
import io.github.booster.messaging.subscriber.aws.AwsSqsSubscriber;
import io.github.booster.messaging.util.TraceHelper;
import io.github.booster.task.Task;
import io.opentelemetry.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.stream.Collectors;

public class AwsSqsBatchProcessor extends AbstractBatchProcessor<Message> {

    private static final Logger log = LoggerFactory.getLogger(AwsSqsBatchProcessor.class);

    private final SqsClient sqsClient;

    private final String queueUrl;

    /**
     * Constructor
     *
     * @param subscriberFlow      {@link BatchSubscriberFlow} to listen to.
     * @param processTask         {@link Task} used to process events.
     * @param openTelemetryConfig {@link OpenTelemetryConfig} OpenTelemetry configuration.
     * @param registry            metrics recording.
     * @param manuallyInjectTrace whether to inject trace from booster code.
     */
    public AwsSqsBatchProcessor(
            BatchSubscriberFlow<Message> subscriberFlow,
            AwsSqsConfig awsSqsConfig,
            Task<List<Message>, List<Message>> processTask,
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
    protected Context createContext(List<Message> records) {
        return TraceHelper.createContext(
                this.openTelemetryConfig,
                records,
                AwsSqsSubscriber.GETTER
        );
    }

    @Override
    protected int acknowledge(List<Message> records) {

        List<DeleteMessageBatchRequestEntry> entries = records.stream()
                .map(record -> DeleteMessageBatchRequestEntry.builder()
                        .id(record.messageId())
                        .receiptHandle(record.receiptHandle())
                        .build()
                ).collect(Collectors.toList());

        DeleteMessageBatchRequest request = DeleteMessageBatchRequest.builder()
                .queueUrl(this.queueUrl)
                .entries(entries)
                .build();

        try {
            DeleteMessageBatchResponse response = sqsClient.deleteMessageBatch(request);

            if (response != null) {
                log.info(
                        "booster-messaging - batch deleted sqs message: [{}]",
                        records.stream().map(Message::messageId).collect(Collectors.toList())
                );

                List<String> ids = response.successful()
                        .stream()
                        .map(DeleteMessageBatchResultEntry::id)
                        .collect(Collectors.toList());
                log.info("booster-messaging - batch deleted messages: [{}] successfully", ids);
                return ids.size();
            }
            log.warn(
                    "booster-messaging - unable to batch delete sqs messages: [{}], no exceptions",
                    records.stream()
                            .map(Message::messageId)
                            .collect(Collectors.toList()));
            return 0;
        } catch (Throwable t) {
            log.error(
                    "booster-messaging - unable to batch delete sqs messages: [{}], with error",
                    records.stream()
                            .map(Message::messageId)
                            .collect(Collectors.toList()),
                    t
            );
            return 0;
        }
    }
}
