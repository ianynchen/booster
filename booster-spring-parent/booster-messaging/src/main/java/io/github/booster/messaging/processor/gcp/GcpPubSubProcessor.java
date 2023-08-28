package io.github.booster.messaging.processor.gcp;

import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.messaging.MessagingMetricsConstants;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.processor.AbstractProcessor;
import io.github.booster.messaging.subscriber.SubscriberFlow;
import io.github.booster.messaging.util.FutureHelper;
import io.github.booster.messaging.util.TraceHelper;
import io.github.booster.task.Task;
import io.opentelemetry.context.Context;

/**
 * GCP pub/sub processor to process GCP pub/sub events coming from {@link SubscriberFlow}
 */
public class GcpPubSubProcessor extends AbstractProcessor<AcknowledgeablePubsubMessage> {

    /**
     * Constructs a {@link GcpPubSubProcessor}
     * @param subscriberFlow {@link SubscriberFlow} to listen to.
     * @param processTask {@link Task} used to process GCP pub/sub events
     * @param openTelemetryConfig {@link OpenTelemetryConfig} for metrics
     * @param registry metrics recording.
     * @param manuallyInjectTrace whether to manually inject trace or rely upon OTEL instrumentation
     */
    public GcpPubSubProcessor(
            SubscriberFlow<AcknowledgeablePubsubMessage> subscriberFlow,
            Task<AcknowledgeablePubsubMessage, AcknowledgeablePubsubMessage> processTask,
            OpenTelemetryConfig openTelemetryConfig,
            MetricsRegistry registry,
            boolean manuallyInjectTrace
    ) {
        super(
                MessagingMetricsConstants.GCP_PUBSUB,
                subscriberFlow,
                processTask,
                openTelemetryConfig,
                registry,
                manuallyInjectTrace
        );
    }

    /**
     * Acknowledges Kafka events once successfully processed.
     * @param record record to be acknowledged.
     * @return true if successfully acknowledged, false otherwise.
     */
    @Override
    protected boolean acknowledge(AcknowledgeablePubsubMessage record) {
        return FutureHelper.fromListenableFutureToBoolean(record.ack());
    }

    @Override
    protected Context createContext(AcknowledgeablePubsubMessage record) {
        return TraceHelper.createContext(this.openTelemetryConfig, record);
    }
}
