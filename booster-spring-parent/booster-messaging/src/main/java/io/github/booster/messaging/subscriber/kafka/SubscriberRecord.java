package io.github.booster.messaging.subscriber.kafka;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.springframework.kafka.support.Acknowledgment;

/**
 * A record is an object consumed by Kafka consumer
 * Booster messaging assumes the records are not
 * auto-acknowledged.
 *
 * @param <T> type of subscriber record.
 */
@Getter
@EqualsAndHashCode
@ToString
public class SubscriberRecord<T> {

    /**
     * Data received from Kafka, cannot be null
     */
    @NonNull
    private final T data;

    /**
     * Acknowledgement object, cannot be null.
     */
    @NonNull
    private final Acknowledgment acknowledgment;

    /**
     * Constructs a {@link SubscriberRecord}
     * @param data message received
     * @param acknowledgment {@link Acknowledgment} object.
     */
    public SubscriberRecord(
            @NonNull T data,
            @NonNull Acknowledgment acknowledgment
    ) {
        this.data = data;
        this.acknowledgment = acknowledgment;
    }
}
