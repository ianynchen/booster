package io.github.booster.messaging.publisher.kafka;

import com.google.api.client.util.Preconditions;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Kafka message to be sent. Kafka publisher assumes
 * key and correlation IDs are of type {@link String}
 * @param <T> type of payload.
 */
@Getter
@EqualsAndHashCode
public class KafkaRecord<T> {

    private final String key;

    private final T value;

    private final String correlationKey;

    /**
     * Constructs a record with correlation ID same as key
     * @param key value for key
     * @param value payload
     */
    public KafkaRecord(String key, T value) {
        this(key, value, key);
    }

    /**
     * Construct a record
     * @param key value for key
     * @param value payload
     * @param correlationKey correlation key, must be of type String
     */
    public KafkaRecord(String key, T value, String correlationKey) {
        Preconditions.checkNotNull(key, "key cannot be null");
        Preconditions.checkNotNull(value, "value cannot be null");
        Preconditions.checkNotNull(correlationKey, "correlation key cannot be null");

        this.key = key;
        this.value = value;
        this.correlationKey = correlationKey;
    }
}
