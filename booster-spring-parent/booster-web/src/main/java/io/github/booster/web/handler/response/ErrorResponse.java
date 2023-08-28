package io.github.booster.web.handler.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.github.booster.web.handler.ExceptionConverter;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.springframework.http.HttpStatus;

/**
 * Error response object in Restful endpoints returns.
 */
@Getter
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(builder = ErrorResponse.ErrorResponseBuilder.class)
public class ErrorResponse {

    /**
     * A more detailed error code
     */
    @JsonProperty("error_code")
    private String errorCode;

    /**
     * Error message, typically exception message
     */
    private String message;

    /**
     * Detailed explanation of the error
     */
    @JsonProperty("details")
    private String detailedReason;

    /**
     * Stack trace. this will be cleared in production.
     */
    @JsonProperty("stack_trace")
    private String stackTrace;

    /**
     * All args constructor
     * @param errorCode error code
     * @param message error message
     * @param detailedReason details reason of error
     * @param stackTrace stack trace associated
     */
    protected ErrorResponse(
            String errorCode,
            String message,
            String detailedReason,
            String stackTrace
    ) {
        this.errorCode = errorCode;
        this.message = message;
        this.detailedReason = detailedReason;
        this.stackTrace = stackTrace;
    }

    /**
     * Builder for {@link ErrorResponse}
     */
    @JsonPOJOBuilder(withPrefix = "")
    public static class ErrorResponseBuilder {

        @JsonProperty("error_code")
        private String errorCode;

        private String message;

        @JsonProperty("details")
        private String detailedReason;

        @JsonProperty("stack_trace")
        private String stackTrace;

        /**
         * Default constructor that does nothing.
         */
        public ErrorResponseBuilder() {
        }

        /**
         * Sets error code
         * @param errorCode error code
         * @return {@link ErrorResponseBuilder}
         */
        public ErrorResponseBuilder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        /**
         * Sets error message
         * @param message error message
         * @return {@link ErrorResponseBuilder}
         */
        public ErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        /**
         * Sets detailed reason for failure
         * @param detailedReason detailed reason for failure
         * @return {@link ErrorResponseBuilder}
         */
        public ErrorResponseBuilder detailedReason(String detailedReason) {
            this.detailedReason = detailedReason;
            return this;
        }

        /**
         * Sets stack trace
         * @param stackTrace stack trace info
         * @return {@link ErrorResponseBuilder}
         */
        public ErrorResponseBuilder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        /**
         * Builds an {@link ErrorResponse}
         * @return {@link ErrorResponse}
         */
        public ErrorResponse build() {
            return new ErrorResponse(
                    errorCode,
                    message,
                    detailedReason,
                    stackTrace
            );
        }

        /**
         * Builds an {@link ErrorResponse} and {@link HttpStatus} from
         * a {@link Throwable} and {@link ExceptionConverter}
         * @param throwable {@link Throwable} to be converted from
         * @param exceptionConverter {@link ExceptionConverter} used to convert
         * @return {@link Tuple2} of {@link ErrorResponse} and {@link HttpStatus}
         */
        public Tuple2<ErrorResponse, HttpStatus> buildFromThrowable(
                @NonNull Throwable throwable,
                @NonNull ExceptionConverter exceptionConverter
        ) {
            WebException webException;
            if (throwable instanceof WebException) {
                webException = (WebException) throwable;
            } else {
                webException = exceptionConverter.handle(throwable);
            }

            return Tuple.of(
                    ErrorResponse.builder()
                            .errorCode(webException.getErrorCode())
                            .message(throwable.getLocalizedMessage())
                            .detailedReason(webException.getDetailedReason())
                            .stackTrace(webException.getShortStackTrace())
                            .build(),
                    webException.getStatus()
            );
        }
    }

    /**
     * Allows stack trace to be cleared in production.
     */
    public void clearStackTrace() {
        this.stackTrace = "";
    }

    /**
     * Creates a {@link ErrorResponseBuilder} instance
     * @return {@link ErrorResponseBuilder}
     */
    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }
}
