package io.github.booster.messaging.subscriber.kafka;

import arrow.core.Option;
import com.google.common.base.Preconditions;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.config.thread.ThreadPoolConfigGeneric;
import io.github.booster.messaging.MessagingMetricsConstants;
import io.github.booster.messaging.config.KafkaSubscriberConfig;
import io.github.booster.messaging.queue.MessageQueue;
import io.github.booster.messaging.subscriber.SubscriberFlow;
import io.github.booster.messaging.util.MetricsHelper;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Flux;

/**
 * Kafka subscriber to be used with spring-kafka listeners.
 * @param <T> Type of payload.
 */
public class KafkaSubscriber<T> implements SubscriberFlow<SubscriberRecord<T>> {

    private static final Logger log = LoggerFactory.getLogger(KafkaSubscriber.class);

    private final MessageQueue<SubscriberRecord<T>> queue;

    private final MetricsRegistry registry;

    private final String name;

    /**
     * Constructs a subscriber
     * @param name name of subscriber. the name is also used to get thread pool to be used for message queue.
     * @param threadPoolConfig {@link ThreadPoolConfigGeneric} to get the thread pool from.
     * @param kafkaSubscriberConfig {@link KafkaSubscriberConfig} that defines message queue capacity.
     * @param registry to record metrics.
     */
    public KafkaSubscriber(
            String name,
            ThreadPoolConfigGeneric threadPoolConfig,
            KafkaSubscriberConfig kafkaSubscriberConfig,
            MetricsRegistry registry
    ) {
        Preconditions.checkArgument(kafkaSubscriberConfig != null, "kafka subscriber config cannot be null");
        Preconditions.checkArgument(kafkaSubscriberConfig.getSetting(name) != null, "kafka subscriber setting cannot be null");

        this.name = name;
        this.registry = registry;
        this.queue = new MessageQueue<>(
                name,
                threadPoolConfig,
                registry,
                kafkaSubscriberConfig.getSetting(name).getQueueSize()
        );
    }

    /**
     * Used by Kafka listener to push messages to queue.
     * @param data data to be pushed
     * @param ack {@link Acknowledgment} object to be used to later acknowledge the event.
     */
    public void push(T data, Acknowledgment ack) {
        Option<Timer.Sample> sample = this.registry.startSample();
        log.debug("booster-messaging - subscriber[{}] received a message [{}], pushing to queue", this.name, data);
        this.queue.push(new SubscriberRecord<>(data, ack));
        MetricsHelper.recordMessageSubscribeCount(
                this.registry,
                MessagingMetricsConstants.SUBSCRIBER_PULL_COUNT,
                MessagingMetricsConstants.KAFKA,
                this.name,
                MessagingMetricsConstants.SUCCESS_STATUS,
                MessagingMetricsConstants.SUCCESS_STATUS
        );
        MetricsHelper.recordProcessingTime(
                this.registry,
                sample,
                MessagingMetricsConstants.SUBSCRIBER_PULL_TIME,
                MessagingMetricsConstants.KAFKA,
                this.name
        );
    }

    /**
     * Returns the messages stored in {@link MessageQueue} as a {@link Flux}
     * @return a {@link Flux} of none null {@link SubscriberRecord}s
     */
    @Override
    public Flux<SubscriberRecord<T>> flatFlux() {
        return this.queue.flux()
                .filter(record -> record != null && record.isDefined() && record.orNull() != null)
                .map(Option::orNull);
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Stops the message queue and threads used.
     */
    public void stop() {
        this.queue.stop();
    }
}
