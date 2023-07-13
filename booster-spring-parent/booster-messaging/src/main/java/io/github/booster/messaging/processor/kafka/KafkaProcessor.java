package io.github.booster.messaging.processor.kafka;

import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.messaging.MessagingMetricsConstants;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.processor.AbstractProcessor;
import io.github.booster.messaging.subscriber.SubscriberFlow;
import io.github.booster.messaging.subscriber.kafka.SubscriberRecord;
import io.github.booster.task.Task;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kafka processor to process Kafka events coming from {@link SubscriberFlow}
 * @param <T> type of Kafka payload.
 */
public class KafkaProcessor<T> extends AbstractProcessor<SubscriberRecord<T>> {

    private final static Logger log = LoggerFactory.getLogger(KafkaProcessor.class);

    /**
     * Constructs a {@link KafkaProcessor}
     * @param subscriberFlow {@link SubscriberFlow} to listen to.
     * @param processTask {@link Task} used to process Kafka events
     * @param registry metrics recording.
     */
    public KafkaProcessor(
            SubscriberFlow<SubscriberRecord<T>> subscriberFlow,
            Task<SubscriberRecord<T>, SubscriberRecord<T>> processTask,
            OpenTelemetryConfig openTelemetryConfig,
            MetricsRegistry registry,
            boolean manuallyInjectTrace
    ) {
        super(
                MessagingMetricsConstants.KAFKA,
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
    protected boolean acknowledge(SubscriberRecord<T> record) {
        if (record != null) {
            log.debug("booster-messaging - acknowledging message in processor[{}], record: {}", this.getName(), record);
            try {
                record.getAcknowledgment().acknowledge();
                return true;
            } catch (Throwable t) {
                log.error("booster-messaging - processor[{}] acknowledgement caused exception, record: {}", this.getName(), record, t);
                return false;
            }
        } else {
            log.debug("booster-messaging - no message to be acknowledged in processor[{}]", this.getName());
            return false;
        }
    }

    @Override
    protected Context createContext(SubscriberRecord<T> record) {
        return Context.current();
    }

    @Override
    protected Span createSpan(Context context) {
        return Span.current();
    }
}
