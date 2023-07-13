package io.github.booster.web.handler.response;

import arrow.core.Either;
import arrow.core.EitherKt;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.base.Preconditions;
import io.github.booster.web.handler.ExceptionConverter;
import io.vavr.Tuple2;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * A generic response object that wraps a normal response, an error
 * section and an optional diagnostics section.
 * @param <T> type of actual response body
 */
@Getter
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(builder = WebResponse.WebResponseBuilder.class)
public class WebResponse<T> {

    private static final Logger log = LoggerFactory.getLogger(WebResponse.class);

    /**
     * Builder class for {@link WebResponse}
     * @param <T> type of actual response body
     */
    @JsonPOJOBuilder(withPrefix = "")
    public static class WebResponseBuilder<T> {

        private T response;

        private ErrorResponse error;

        /**
         * Sets response object
         * @param response response body
         * @return {@link WebResponseBuilder}
         */
        public WebResponseBuilder<T> response(T response) {
            this.response = response;
            return this;
        }

        /**
         * Sets error response
         * @param error error response body
         * @return {@link WebResponseBuilder}
         */
        public WebResponseBuilder<T> error(ErrorResponse error) {
            this.error = error;
            return this;
        }

        /**
         * Builds {@link WebResponse}. Error will be thrown if both
         * response and error are null or if both are non-null.
         * @return {@link WebResponse}
         */
        public WebResponse<T> build() {
            Preconditions.checkArgument(error == null || response == null, "response and error cannot both be non-null");
            Preconditions.checkArgument(error != null || response != null, "response and error cannot both be null");
            return new WebResponse<>(this.response, this.error);
        }
    }

    private final T response;

    private final ErrorResponse error;

    protected WebResponse(
            T response,
            ErrorResponse error
    ) {
        this.error = error;
        this.response = response;
    }

    /**
     * Creates builder for {@link WebResponse}
     * @return {@link WebResponseBuilder}
     * @param <T> type of actual response body.
     */
    public static <T> WebResponseBuilder<T> builder() {
        return new WebResponseBuilder<>();
    }

    /**
     * Converts an {@link Either} into a {@link ResponseEntity}
     * @param either {@link Either} to be converted
     * @param exceptionConverter {@link ExceptionConverter} to convert exceptions into corresponding
     *                                                     http status and error messages
     * @return {@link ResponseEntity}
     * @param <T> type of actual response
     */
    public static <T> ResponseEntity<WebResponse<?>> build(
            Either<Throwable, T> either,
            ExceptionConverter exceptionConverter
    ) {
        if (either == null) {
            log.warn("booster-web - either is null, building empty response body");
            return ResponseEntity.status(HttpStatus.OK.value()).build();
        } else if (either.isRight()) {
            log.debug("booster-web - either is right value: [{}], building response body", either);
            return ResponseEntity.status(HttpStatus.OK.value())
                    .body(
                            WebResponse.builder()
                                    .response(EitherKt.getOrElse(either, o -> null))
                                    .build()
                    );
        } else {
            log.debug("booster-web - either is left value: [{}], building error response", either);
            Throwable t = either.swap().getOrNull();

            if (t == null) {
                log.warn("booster-web - either is left, but no error, building empty response body");
                return ResponseEntity.status(HttpStatus.OK.value()).build();
            }
            Tuple2<ErrorResponse, HttpStatus> errorTuple = ErrorResponse.builder()
                    .buildFromThrowable(t, exceptionConverter);
            return ResponseEntity.status(errorTuple._2())
                    .body(WebResponse.builder().error(errorTuple._1()).build());
        }
    }
}