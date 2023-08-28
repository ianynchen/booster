package io.github.booster.web.handler;

import io.github.booster.web.handler.response.WebException;
import lombok.NonNull;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;

/**
 * Converts a java {@link Throwable} to {@link WebException}
 * @param <T> type of {@link Throwable}
 */
public interface ExceptionHandler<T extends Throwable> {

    /**
     * Converts a generic Java exception to {@link WebException}
     * @param throwable generic Java exception.
     * @return {@link WebException}
     */
    default WebException convert(@NonNull Throwable throwable) {
        if (this.canHandle(throwable)) {
            return this.handle((T)throwable);
        }
        return null;
    }

    /**
     * Converts a generic Java exception to {@link WebException}
     * @param throwable generic Java exception
     * @return {@link WebException}
     */
    WebException handle(@NonNull T throwable);

    /**
     * Determines if a particular instance of {@link Throwable} can be handled.
     * A {@link Throwable} can be handled by an {@link ExceptionHandler} instance if
     * the {@link Throwable} is of the exact type or subclass of type T.
     * @param t {@link Throwable} to be checked
     * @return true if the {@link Throwable} is of the exact same type or subclass of
     *         the type specified by the {@link ExceptionHandler}, false otherwise.
     */
    default boolean canHandle(Throwable t) {
        return this.handles().isAssignableFrom(t.getClass());
    }

    /**
     * Creates a {@link WebException} with custom {@link HttpStatus} and error code
     * @param throwable the exception to be converted
     * @param httpStatus {@link HttpStatus} to be used
     * @param errorCode error code to be used
     * @return {@link WebException}
     */
    default WebException createResponse(
            @NonNull Throwable throwable,
            @NonNull HttpStatus httpStatus,
            @NonNull String errorCode
    ) {
        return new WebException(
                httpStatus,
                errorCode,
                throwable.getLocalizedMessage(),
                ExceptionUtils.getStackTrace(throwable)
        );
    }

    /**
     * Returns the type of {@link Throwable} an {@link ExceptionHandler} handles
     * @return {@link Class} of {@link Throwable} the handler handles.
     */
    Class<T> handles();
}
