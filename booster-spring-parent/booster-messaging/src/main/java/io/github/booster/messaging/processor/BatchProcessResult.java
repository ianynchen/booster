package io.github.booster.messaging.processor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * Batch process result
 * @param <T> type of payload
 */
@Getter
@EqualsAndHashCode
@ToString
public class BatchProcessResult<T> {

    /**
     * Data in the batch
     */
    private final List<T> data;

    /**
     * Number of acknowledged messages per batch
     */
    private final int acknowledged;

    /**
     * Number of unacknowledged messages per batch.
     * All failed messages will be unacknowledged
     */
    private final int unacknowledged;

    /**
     * Number of messages failed to be processed.
     */
    private final int failed;

    /**
     * Constructs a {@link BatchProcessResult}
     * @param data batch data
     * @param acknowledged number of acknowledged messages
     * @param unacknowledged number of unacknowledged messages
     * @param failed number of messages failed processing
     */
    public BatchProcessResult(
            List<T> data,
            int acknowledged,
            int unacknowledged,
            int failed
    ) {
        this.data = data;
        this.acknowledged = acknowledged;
        this.unacknowledged = unacknowledged;
        this.failed = failed;
    }
}
