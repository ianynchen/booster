package io.github.booster.messaging.subscriber.kafka;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.springframework.kafka.support.Acknowledgment;

/**
 * A record is an object consumed by Kafka consumer
 * Booster messaging assumes the records are not
 * auto-acknowledged.
 */
@Getter
@EqualsAndHashCode
@AllArgsConstructor
@ToString
public class SubscriberRecord<T> {

    /**
     * Data received from Kafka, cannot be null
     */
    @NonNull
    private T data;

    /**
     * Acknowledgement object, cannot be null.
     */
    @NonNull
    private Acknowledgment acknowledgment;
}
