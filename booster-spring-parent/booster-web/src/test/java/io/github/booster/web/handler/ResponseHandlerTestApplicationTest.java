package io.github.booster.web.handler;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResponseHandlerTestApplicationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldGetGreeting() {
        this.webTestClient.get().uri(builder -> {
            builder.queryParam("from", "john");
            builder.queryParam("greeting", "hello");
            builder.path("/api/v1/hello");
            return builder.build();
        }).accept(MediaType.APPLICATION_JSON).exchange().expectBody()
                .jsonPath("$.response").exists()
                .jsonPath("$.error").doesNotExist()
                .jsonPath("$.response.from").value(equalTo("server"))
                .jsonPath("$.response.to").value(equalTo("john"))
                .jsonPath("$.response.greeting").value(equalTo("hello"));

        this.webTestClient.get().uri(builder -> {
                    builder.queryParam("from", "death");
                    builder.queryParam("greeting", "hello");
                    builder.path("/api/v1/hello");
                    return builder.build();
                }).accept(MediaType.APPLICATION_JSON).exchange().expectBody()
                .jsonPath("$.response").doesNotExist()
                .jsonPath("$.error").exists()
                .jsonPath("$.error.error_code").value(equalTo("INTERNAL_SERVER_ERROR"));

        this.webTestClient.get().uri(builder -> {
                    builder.queryParam("from", "abc");
                    builder.queryParam("greeting", "hola");
                    builder.path("/api/v1/hello");
                    return builder.build();
                }).accept(MediaType.APPLICATION_JSON).exchange().expectBody()
                .jsonPath("$.response").doesNotExist()
                .jsonPath("$.error").exists()
                .jsonPath("$.error.error_code").value(equalTo("BAD_REQUEST"));
    }
}
