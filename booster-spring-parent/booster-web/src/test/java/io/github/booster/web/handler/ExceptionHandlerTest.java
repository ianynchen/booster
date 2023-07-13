package io.github.booster.web.handler;

import io.github.booster.web.handler.response.WebException;
import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExceptionHandlerTest {

    static class IllegalArgumentExceptionHandler implements ExceptionHandler<IllegalArgumentException> {

        @Override
        public WebException handle(@NonNull IllegalArgumentException exception) {
            return this.createResponse(exception, HttpStatus.BAD_GATEWAY, HttpStatus.BAD_GATEWAY.toString());
        }

        @Override
        public Class<IllegalArgumentException> handles() {
            return IllegalArgumentException.class;
        }
    }

    static class SubException extends IllegalArgumentException {

    }

    @Test
    void shouldBeAbleToHandle() {
        IllegalArgumentExceptionHandler handler = new IllegalArgumentExceptionHandler();
        assertThat(handler.canHandle(new IllegalArgumentException()), equalTo(true));
        assertThat(handler.canHandle(new SubException()), equalTo(true));
    }

    @Test
    void shouldNotHandle() {
        IllegalArgumentExceptionHandler handler = new IllegalArgumentExceptionHandler();
        assertThat(handler.canHandle(new IllegalStateException()), equalTo(false));
    }

    @Test
    void shouldReturnNull() {
        IllegalArgumentExceptionHandler handler = new IllegalArgumentExceptionHandler();
        assertThat(handler.convert(new IllegalStateException()), nullValue());
    }

    @Test
    void shouldHandleSameClass() {
        IllegalArgumentExceptionHandler handler = new IllegalArgumentExceptionHandler();
        assertThat(handler.convert(new IllegalArgumentException()), notNullValue());
    }

    @Test
    void shouldHandleSubClass() {
        IllegalArgumentExceptionHandler handler = new IllegalArgumentExceptionHandler();
        assertThat(handler.convert(new SubException()), notNullValue());
    }

    @Test
    void shouldFailCreateException() {
        IllegalArgumentExceptionHandler handler = new IllegalArgumentExceptionHandler();
        assertThrows(
                NullPointerException.class,
                () -> handler.createResponse(null, HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.name())
        );
        assertThrows(
                NullPointerException.class,
                () -> handler.createResponse(new IllegalStateException(), null, HttpStatus.INTERNAL_SERVER_ERROR.name())
        );
        assertThrows(
                NullPointerException.class,
                () -> handler.createResponse(new IllegalStateException(), HttpStatus.INTERNAL_SERVER_ERROR, null)
        );
    }

    @Test
    void shouldFail() {
        IllegalArgumentExceptionHandler handler = new IllegalArgumentExceptionHandler();
        assertThrows(
                NullPointerException.class,
                () -> handler.convert(null)
        );
    }
}
