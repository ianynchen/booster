package io.github.booster.messaging.publisher;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Publisher record for successfully publishing to GCP or Kafka.
 */
@Getter
@EqualsAndHashCode
@Builder
@ToString
public class PublisherRecord {

    /**
     * Topic to publish to
     */
    private String topic;

    /**
     * Record ID returned.
     */
    private String recordId;
}
