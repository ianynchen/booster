package io.github.booster.web.handler.response;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;

/**
 * Exception that contains fail reasons and error code. This exception
 * is used to generate HTTP response status codes for endpoints.
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public class WebException extends RuntimeException {

    /**
     * Status code that should be returned
     */
    private final HttpStatus status;

    /**
     * Error code
     */
    private final String errorCode;

    /**
     * Detailed reason of failure
     */
    private final String detailedReason;

    /**
     * Stack trace of the exception
     */
    private final String shortStackTrace;

    /**
     * Constructor
     * @param status Http status code
     * @param errorCode error code to be used
     * @param detailedReason detailed failure reason
     * @param shortStackTrace stack trace
     */
    public WebException(
            HttpStatus status,
            String errorCode,
            String detailedReason,
            String shortStackTrace
    ) {
        super(detailedReason);
        this.status = status;
        this.errorCode = errorCode;
        this.detailedReason = detailedReason;
        this.shortStackTrace = shortStackTrace;
    }

    /**
     * Constructor with embedded exception as cause
     * @param status Http status code
     * @param errorCode error code
     * @param cause embedded exception
     */
    public WebException(
            HttpStatus status,
            String errorCode,
            Throwable cause
    ) {
        super(cause.getLocalizedMessage(), cause);
        this.status = status;
        this.errorCode = errorCode;
        this.detailedReason = cause.getLocalizedMessage();
        this.shortStackTrace = ExceptionUtils.getStackTrace(cause);
    }
}
