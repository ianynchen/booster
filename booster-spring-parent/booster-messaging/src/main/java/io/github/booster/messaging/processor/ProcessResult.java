package io.github.booster.messaging.processor;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * Message processing result object
 * @param <T> type of message body
 */
@Getter
@EqualsAndHashCode
@ToString
public class ProcessResult<T> {

    /**
     * Message body
     */
    @NonNull
    private final T data;

    /**
     * whether the processed message was acknowledged successfully
     */
    private final boolean acknowledged;

    /**
     * Constructs a {@link ProcessResult}
     * @param data message processed
     * @param acknowledged whether message is acknowledged.
     */
    public ProcessResult(
            @NotNull T data,
            boolean acknowledged
    ) {
        this.data = data;
        this.acknowledged = acknowledged;
    }
}
