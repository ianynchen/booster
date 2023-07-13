package io.github.booster.web.handler.response;

import arrow.core.Either;
import io.github.booster.web.handler.ExceptionConverter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

class WebResponseTest {

    @Test
    void shouldBuildEmptyResponse() {
        ResponseEntity<WebResponse<?>> response = WebResponse.build(null, new ExceptionConverter(null));
        assertThat(response, notNullValue());
        assertThat(response.getBody(), nullValue());
    }

    @Test
    void shouldBuildEmptyResponseFromNullException() {
        ResponseEntity<WebResponse<?>> response = WebResponse.build(new Either.Left<>(null), new ExceptionConverter(null));
        assertThat(response, notNullValue());
        assertThat(response.getBody(), nullValue());
    }

    @Test
    void shouldBuildResponseBody() {
        Either<Throwable, String> content = new Either.Right<>("abc");
        ResponseEntity<WebResponse<?>> response = WebResponse.build(content, new ExceptionConverter(null));
        assertThat(response, notNullValue());
        assertThat(response.getBody(), notNullValue());
        assertThat(response.getBody().getResponse(), equalTo("abc"));
    }

    @Test
    void shouldBuildError() {
        Either<Throwable, String> content = new Either.Left<>(new IllegalArgumentException());
        ResponseEntity<WebResponse<?>> response = WebResponse.build(content, new ExceptionConverter(null));
        assertThat(response, notNullValue());
        assertThat(response.getBody(), notNullValue());
        assertThat(response.getBody().getResponse(), nullValue());
        assertThat(response.getBody().getError(), notNullValue());
        assertThat(response.getStatusCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }
}
