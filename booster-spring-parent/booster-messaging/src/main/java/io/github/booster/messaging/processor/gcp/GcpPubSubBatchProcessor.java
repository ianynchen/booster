package io.github.booster.messaging.processor.gcp;

import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.messaging.MessagingMetricsConstants;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.processor.AbstractBatchProcessor;
import io.github.booster.messaging.subscriber.BatchSubscriberFlow;
import io.github.booster.messaging.subscriber.gcp.GcpPubSubPullSubscriber;
import io.github.booster.messaging.util.FutureHelper;
import io.github.booster.messaging.util.TraceHelper;
import io.github.booster.task.Task;
import io.opentelemetry.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Processes GCP pub/sub events in batch mode. Each element will be a list of
 * {@link AcknowledgeablePubsubMessage} objects.
 */
public class GcpPubSubBatchProcessor extends AbstractBatchProcessor<AcknowledgeablePubsubMessage> {

    private final static Logger log = LoggerFactory.getLogger(GcpPubSubBatchProcessor.class);

    /**
     * Constructs a {@link GcpPubSubBatchProcessor}
     * @param subscriberFlow {@link BatchSubscriberFlow} to listen to.
     * @param processTask {@link Task} used to process GCP pub/sub events
     * @param openTelemetryConfig {@link OpenTelemetryConfig} for metrics
     * @param registry metrics recording.
     * @param manuallyInjectTrace whether to manually inject trace or rely upon OTEL instrumentation
     */
    public GcpPubSubBatchProcessor(
            BatchSubscriberFlow<AcknowledgeablePubsubMessage> subscriberFlow,
            Task<List<AcknowledgeablePubsubMessage>, List<AcknowledgeablePubsubMessage>> processTask,
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

    @Override
    protected Context createContext(List<AcknowledgeablePubsubMessage> records) {
        return TraceHelper.createContext(this.openTelemetryConfig, records, GcpPubSubPullSubscriber.GETTER);
    }

    /**
     * Acknowledges a list of events.
     * @param records records to be acknowledged.
     * @return list of events that are acknowledged
     */
    @Override
    protected int acknowledge(List<AcknowledgeablePubsubMessage> records) {
        return records.stream()
                .map(record -> FutureHelper.fromListenableFutureToBoolean(record.ack()))
                .mapToInt(isAcknowledged -> {
                    if (isAcknowledged) {
                        return 1;
                    }
                    return 0;
                }).sum();
    }
}
