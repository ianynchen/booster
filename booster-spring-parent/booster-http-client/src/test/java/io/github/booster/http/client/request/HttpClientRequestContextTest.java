package io.github.booster.http.client.request;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpClientRequestContextTest {

    @Test
    void shouldNotAllowNullMethod() {
        assertThrows(
                IllegalArgumentException.class,
                () -> HttpClientRequestContext.builder().build()
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> HttpClientRequestContext.builder().requestMethod(null).build()
        );
    }

    @Test
    void shouldCreateEmptyContext() {
        HttpClientRequestContext context = HttpClientRequestContext.builder().requestMethod(HttpMethod.GET).build();
        assertThat(context, notNullValue());
        assertThat(context.getRequest(), nullValue());
        assertThat(context.getHeaders(), notNullValue());
        assertThat(context.getHeaders().entrySet(), hasSize(0));
        assertThat(context.getPath(), notNullValue());
        assertThat(context.getPath(), equalTo(""));
        assertThat(context.getPathVariables().size(), equalTo(0));
        assertThat(context.getRequestReference(), nullValue());
        assertThat(context.getQueryParameters().size(), equalTo(0));
        assertThat(context.getResponseClass(), nullValue());
        assertThat(context.getResponseReference(), nullValue());
    }

    @Test
    void shouldCreateContext() {
        HttpClientRequestContext context = HttpClientRequestContext.builder()
                .requestMethod(HttpMethod.GET)
                .path("abc")
                .pathVariables(Map.of("var1", "value1"))
                .queryParameters(Map.of("query", List.of("value1", "value2")))
                .build();

        assertThat(context, notNullValue());
        assertThat(context.getRequest(), nullValue());
        assertThat(context.getHeaders(), notNullValue());
        assertThat(context.getHeaders().entrySet(), hasSize(0));
        assertThat(context.getPath(), equalTo("abc"));
        assertThat(context.getPathVariables().size(), equalTo(1));

        Set<String> keyset = context.getPathVariables().keySet();
        assertThat(keyset, contains("var1"));

        assertThat(context.getQueryParameters().size(), equalTo(1));

        keyset = context.getQueryParameters().keySet();
        assertThat(keyset, contains("query"));

        assertThat(context.getRequestReference(), nullValue());
        assertThat(context.getResponseClass(), nullValue());
        assertThat(context.getResponseReference(), nullValue());
    }
}
