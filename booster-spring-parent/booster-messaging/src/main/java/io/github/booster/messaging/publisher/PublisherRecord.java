package io.github.booster.messaging.publisher;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Publisher record for successfully publishing to GCP or Kafka.
 */
@Getter
@EqualsAndHashCode
@ToString
public class PublisherRecord {

    /**
     * Builder class for {@link PublisherRecord}
     */
    public static class Builder {

        /**
         * Topic to publish to
         */
        private String topic;

        /**
         * Record ID returned.
         */
        private String recordId;

        /**
         * Default constructor
         */
        protected Builder() {
        }

        /**
         * Sets topic
         * @param topic topic
         * @return {@link Builder}
         */
        public Builder topic(String topic) {
            this.topic = topic;
            return this;
        }

        /**
         * Sets record ID
         * @param recordId record ID
         * @return {@link Builder}
         */
        public Builder recordId(String recordId) {
            this.recordId = recordId;
            return this;
        }

        /**
         * Builds a {@link PublisherRecord}
         * @return returns a {@link PublisherRecord}
         */
        public PublisherRecord build() {
            return new PublisherRecord(
                    this.topic,
                    this.recordId
            );
        }
    }

    /**
     * Topic to publish to
     */
    private final String topic;

    /**
     * Record ID returned.
     */
    private final String recordId;

    /**
     * All param constructor
     * @param topic topic for publisher record
     * @param recordId returned by publisher if successful
     */
    protected PublisherRecord(
            String topic,
            String recordId
    ) {
        this.topic = topic;
        this.recordId = recordId;
    }

    /**
     * Creates {@link Builder}
     * @return {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }
}
