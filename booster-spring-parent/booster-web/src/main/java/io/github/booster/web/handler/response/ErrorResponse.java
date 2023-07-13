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

@Getter
@AllArgsConstructor
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

    @JsonPOJOBuilder(withPrefix = "")
    public static class ErrorResponseBuilder {

        @JsonProperty("error_code")
        private String errorCode;

        private String message;

        @JsonProperty("details")
        private String detailedReason;

        @JsonProperty("stack_trace")
        private String stackTrace;

        protected ErrorResponseBuilder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        protected ErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        protected ErrorResponseBuilder detailedReason(String detailedReason) {
            this.detailedReason = detailedReason;
            return this;
        }

        protected ErrorResponseBuilder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        protected ErrorResponse build() {
            return new ErrorResponse(
                    errorCode,
                    message,
                    detailedReason,
                    stackTrace
            );
        }

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
     * Allows stack trace to be cleared in productoin.
     */
    public void clearStackTrace() {
        this.stackTrace = "";
    }

    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }
}
