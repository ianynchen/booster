package io.github.booster.messaging.publisher.gcp;

import com.google.api.client.util.Preconditions;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Map;

/**
 * GCP pub/sub record to be sent by {@link GcpPubSubPublisher}
 * @param <T> type of payload
 */
@Getter
@EqualsAndHashCode
public class PubsubRecord<T> {

    private final T payload;

    private final Map<String, String> headers;

    /**
     * Constructs a {@link PubsubRecord} without any headers.
     * @param payload payload, cannot be null
     */
    public PubsubRecord(T payload) {
        this(payload, Map.of());
    }

    /**
     * Constructs a {@link PubsubRecord} with headers.
     * @param payload payload, cannot be null
     * @param headers headers for the mssage.
     */
    public PubsubRecord(T payload, Map<String, String> headers) {
        Preconditions.checkNotNull(payload, "payload cannot be null");
        Preconditions.checkNotNull(headers, "headers cannot be null");
        this.payload = payload;
        this.headers = headers;
    }
}
