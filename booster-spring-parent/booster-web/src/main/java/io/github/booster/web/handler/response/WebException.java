package io.github.booster.web.handler.response;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.springframework.http.HttpStatus;

@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
public class WebException extends RuntimeException {

    private final HttpStatus status;

    private final String errorCode;

    private final String detailedReason;

    private final String shortStackTrace;

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

    public WebException(
            HttpStatus status,
            String errorCode,
            String detailedReason,
            String shortStackTrace,
            Throwable cause
    ) {
        super(detailedReason, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.detailedReason = detailedReason;
        this.shortStackTrace = shortStackTrace;
    }
}
