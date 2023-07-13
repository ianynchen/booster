package io.github.booster.web.handler;

import io.github.booster.web.handler.response.WebException;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class ExceptionConverterTest {

    static class SubException extends IllegalArgumentException {

    }

    static class IllegalArgumentExceptionHandler implements ExceptionHandler<IllegalArgumentException> {

        @Override
        public WebException handle(@NonNull IllegalArgumentException exception) {
            return this.createResponse(
                    exception,
                    HttpStatus.SERVICE_UNAVAILABLE,
                    HttpStatus.SERVICE_UNAVAILABLE.name()
            );
        }

        @Override
        public Class<IllegalArgumentException> handles() {
            return IllegalArgumentException.class;
        }
    }

    static class IllegalStateExceptionHandler implements ExceptionHandler<IllegalStateException> {

        @Override
        public WebException handle(@NonNull IllegalStateException exception) {
            return this.createResponse(
                    exception,
                    HttpStatus.BAD_GATEWAY,
                    HttpStatus.BAD_GATEWAY.name()
            );
        }

        @Override
        public Class<IllegalStateException> handles() {
            return IllegalStateException.class;
        }
    }

    private List<ExceptionHandler<?>> handlers = List.of(
            new IllegalStateExceptionHandler(),
            new IllegalArgumentExceptionHandler()
    );

    private final ExceptionConverter converter = new ExceptionConverter(handlers);

    @Test
    void shouldHandle() {
        WebException exception = converter.handle(new IllegalStateException());
        assertThat(exception, notNullValue());
        assertThat(exception.getStatus(), equalTo(HttpStatus.BAD_GATEWAY));
        assertThat(exception.getErrorCode(), equalTo(HttpStatus.BAD_GATEWAY.name()));

        exception = converter.handle(new IllegalArgumentException());
        assertThat(exception, notNullValue());
        assertThat(exception.getStatus(), equalTo(HttpStatus.SERVICE_UNAVAILABLE));
        assertThat(exception.getErrorCode(), equalTo(HttpStatus.SERVICE_UNAVAILABLE.name()));

        exception = converter.handle(new ExceptionHandlerTest.SubException());
        assertThat(exception, notNullValue());
        assertThat(exception.getStatus(), equalTo(HttpStatus.SERVICE_UNAVAILABLE));
        assertThat(exception.getErrorCode(), equalTo(HttpStatus.SERVICE_UNAVAILABLE.name()));
    }

    @Test
    void shouldHandleDefault() {
        WebException exception = converter.handle(new NullPointerException());
        assertThat(exception, notNullValue());
        assertThat(exception.getStatus(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(exception.getErrorCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR.name()));
    }
}
