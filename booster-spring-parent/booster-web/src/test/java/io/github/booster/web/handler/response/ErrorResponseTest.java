package io.github.booster.web.handler.response;

import io.github.booster.web.handler.ExceptionConverter;
import io.vavr.Tuple2;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ErrorResponseTest {

    private final ExceptionConverter converter = new ExceptionConverter(null);

    @Test
    void shouldNotBuild() {
        assertThrows(
                NullPointerException.class,
                () -> ErrorResponse.builder().buildFromThrowable(null, converter)
        );
        assertThrows(
                NullPointerException.class,
                () -> ErrorResponse.builder().buildFromThrowable(new IllegalArgumentException(), null)
        );
    }

    @Test
    void shouldBuild() {
        WebException exception = new WebException(
                HttpStatus.ALREADY_REPORTED,
                HttpStatus.ALREADY_REPORTED.toString(),
                new IllegalArgumentException("error")
        );

        Tuple2<ErrorResponse, HttpStatus> response = ErrorResponse.builder().buildFromThrowable(exception, converter);
        assertThat(response._1().getErrorCode(), equalTo(HttpStatus.ALREADY_REPORTED.toString()));
        assertThat(response._1().getDetailedReason(), equalTo("error"));
        assertThat(response._1().getStackTrace(), startsWith("java.lang.IllegalArgumentException: error"));
    }
}
