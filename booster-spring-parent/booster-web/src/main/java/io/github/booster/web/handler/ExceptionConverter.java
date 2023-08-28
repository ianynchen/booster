package io.github.booster.web.handler;

import io.github.booster.web.handler.response.WebException;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Holds all {@link ExceptionHandler} available and converts any {@link Throwable} to
 * a {@link WebException}
 */
public class ExceptionConverter {

    /**
     * A default {@link ExceptionHandler} in case a {@link Throwable} cannot
     * be handled by any of the registered {@link ExceptionHandler}s
     */
    public static class GenericExceptionHandler implements ExceptionHandler<Throwable> {

        /**
         * Constructs a default {@link ExceptionHandler} for all
         * {@link Throwable}
         */
        public GenericExceptionHandler() {
            super();
        }

        /**
         * Handles any {@link Throwable} to generate an internal server error
         * @param exception generic Java exception
         * @return {@link WebException}
         */
        @Override
        public WebException handle(@NonNull Throwable exception) {
            return this.createResponse(
                    exception,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR.name()
            );
        }

        /**
         * Handles all {@link Throwable}
         * @return {@link Class} of {@link Throwable}
         */
        @Override
        public Class<Throwable> handles() {
            return Throwable.class;
        }
    }

    private final List<ExceptionHandler<? extends Throwable>> exceptionHandlers;

    private final GenericExceptionHandler genericExceptionHandler = new GenericExceptionHandler();

    /**
     * Constructor with a list of {@link ExceptionHandler}s
     * @param exceptionHandlers {@link ExceptionHandler}s
     */
    public ExceptionConverter(
            List<ExceptionHandler<? extends Throwable>> exceptionHandlers
    ) {
        this.exceptionHandlers = CollectionUtils.isEmpty(exceptionHandlers) ?
                List.of() :
                exceptionHandlers;
    }

    /**
     * Finds the correct {@link ExceptionHandler} to convert a {@link Throwable} to
     * a {@link WebException}
     * @param throwable any {@link Throwable}
     * @return {@link WebException}
     */
    public WebException handle(Throwable throwable) {
        WebException result = null;

        for (ExceptionHandler<?> handler: this.exceptionHandlers) {
            if (handler.canHandle(throwable)) {
                result = handler.convert(throwable);
                break;
            }
        }
        if (result == null) {
            return this.genericExceptionHandler.convert(throwable);
        }
        return result;
    }
}
