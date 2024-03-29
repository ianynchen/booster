package io.github.booster.messaging.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Kafka subscriber setting.
 */
@Getter
@EqualsAndHashCode
@ToString
public class KafkaSubscriberSetting {

    /**
     * Default constructor that does nothing.
     */
    public KafkaSubscriberSetting() {
    }

    /**
     * message queue size, at least 1
     */
    private int queueSize = 1;

    /**
     * Sets queue size
     * @param queueSize if less than 1, set to 1, otherwise set to queueSize.
     */
    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize <= 0 ? 1 : queueSize;
    }
}
