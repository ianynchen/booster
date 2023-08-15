package io.github.booster.http.client.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.booster.http.client.HttpClient;
import io.github.booster.http.client.HttpClientConfigTestData;
import io.github.booster.http.client.MockApplication;
import io.github.booster.http.client.config.HttpClientConnectionConfig;
import io.github.booster.http.client.config.HttpClientConnectionSetting;
import io.github.booster.http.client.config.MockConfig;
import io.github.booster.http.client.controller.MockController;
import io.github.booster.http.client.request.HttpClientRequestContext;
import io.github.booster.http.client.request.UserContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.web.reactive.client.WebClientExchangeTagsProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = {MockApplication.class, MockConfig.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HttpClientImplTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private WebClientExchangeTagsProvider webClientExchangeTagsProvider;

    private final WebClient webClient = mock(WebClient.class);

    private HttpClientConnectionConfig createConfig() {
        HttpClientConnectionSetting setting = new HttpClientConnectionSetting();
        setting.setBaseUrl("http://127.0.0.1:" + this.port + "/v1/hello");
        setting.setConnectionTimeoutMillis(1000);
        setting.setMaxInMemorySizeMB(1000);
        setting.setReadTimeoutMillis(5000);
        setting.setUseSSL(false);
        setting.setResponseTimeoutInMillis(1000L);
        setting.setWriteTimeoutMillis(5000);

        HttpClientConnectionSetting.ConnectionPoolSetting poolSetting = new HttpClientConnectionSetting.ConnectionPoolSetting();
        poolSetting.setMaxConnections(1000);
        poolSetting.setMaxIdleTimeMillis(1000L);
        poolSetting.setMaxLifeTimeMillis(1000L);
        setting.setPool(poolSetting);

        HttpClientConnectionConfig config = new HttpClientConnectionConfig(this.applicationContext);
        config.setSettings(Map.of("test", setting));
        return config;
    }

    @Test
    void shouldGetResponseByClass() {
        HttpClient<Object, MockController.Greeting> client = new HttpClientImpl<Object, MockController.Greeting>(
                WebClient.builder(),
                "test",
                createConfig()
        );

        HttpClientRequestContext<Object, MockController.Greeting> request = HttpClientRequestContext.<Object, MockController.Greeting>builder()
                .requestMethod(HttpMethod.GET)
                .path("/{name}")
                .pathVariables(Map.of("name", "Jim"))
                .responseClass(MockController.Greeting.class)
                .userContext(
                        UserContext.builder().acceptLanguage("en").build()
                ).build();

        verifyResult(client.invoke(request));
    }

    @Test
    void shouldGetResponseByReference() {
        HttpClient<Object, MockController.Greeting> client = new HttpClientImpl<>(
                WebClient.builder(),
                "test",
                createConfig()
        );

        HttpClientRequestContext<Object, MockController.Greeting> request = HttpClientRequestContext.<Object, MockController.Greeting>builder()
                .requestMethod(HttpMethod.GET)
                .path("/{name}")
                .pathVariables(Map.of("name", "Jim"))
                .responseReference(new ParameterizedTypeReference<MockController.Greeting>() {})
                .userContext(
                        UserContext.builder().acceptLanguage("en").build()
                ).build();

        verifyResult(client.invoke(request));
    }

    @Test
    void shouldAcceptQueryParameter() {
        HttpClient<Object, MockController.Greeting> client = new HttpClientImpl<>(
                WebClient.builder(),
                "test",
                createConfig()
        );

        HttpClientRequestContext<Object, MockController.Greeting> request = HttpClientRequestContext.<Object, MockController.Greeting>builder()
                .requestMethod(HttpMethod.GET)
                .path("/query")
                .queryParameters(
                        Map.of("name", List.of("Jim"))
                )
                .responseClass(MockController.Greeting.class)
                .userContext(
                        UserContext.builder().acceptLanguage("en").build()
                ).build();

        verifyResult(client.invoke(request));
    }

    @Test
    void shouldGetResponseWithNullRequest() {
        HttpClient<MockController.Greeting, MockController.Greeting> client = new HttpClientImpl<>(
                WebClient.builder(),
                "test",
                createConfig()
        );

        HttpClientRequestContext<MockController.Greeting, MockController.Greeting> request = HttpClientRequestContext.<MockController.Greeting, MockController.Greeting>builder()
                .requestMethod(HttpMethod.GET)
                .path("/query")
                .queryParameters(
                        Map.of("name", List.of("Jim"))
                )
                .requestReference(new ParameterizedTypeReference<>() {})
                .responseClass(MockController.Greeting.class)
                .userContext(
                        UserContext.builder().acceptLanguage("en").build()
                ).build();

        verifyResult(client.invoke(request));
    }

    @Test
    void shouldGetResponseWithNonNullRequest() {
        HttpClient<MockController.Greeting, MockController.Greeting> client = new HttpClientImpl<>(
                WebClient.builder(),
                "test",
                createConfig()
        );

        HttpClientRequestContext<MockController.Greeting, MockController.Greeting> request = HttpClientRequestContext.<MockController.Greeting, MockController.Greeting>builder()
                .requestMethod(HttpMethod.POST)
                .path("/")
                .request(new MockController.Greeting("", "Jim", "Server"))
                .responseReference(new ParameterizedTypeReference<>() {})
                .userContext(
                        UserContext.builder().acceptLanguage("en").build()
                ).build();

        verifyResult(client.invoke(request));
    }

    @Test
    void shouldGetResponseWithNullRequest2() {
        HttpClient<MockController.Greeting, MockController.Greeting> client = new HttpClientImpl<>(
                WebClient.builder(),
                "test",
                createConfig()
        );

        HttpClientRequestContext<MockController.Greeting, MockController.Greeting> request = HttpClientRequestContext.<MockController.Greeting, MockController.Greeting>builder()
                .requestMethod(HttpMethod.GET)
                .path("/query")
                .queryParameters(
                        Map.of("name", List.of("Jim"))
                )
                .requestReference(new ParameterizedTypeReference<>() {})
                .responseReference(new ParameterizedTypeReference<>() {})
                .userContext(
                        UserContext.builder().acceptLanguage("en").build()
                ).build();

        verifyResult(client.invoke(request));
    }

    @Test
    void shouldGetPostResponseByClass() {
        HttpClient<MockController.Greeting, MockController.Greeting> client = new HttpClientImpl<>(
                WebClient.builder(),
                "test",
                createConfig()
        );

        HttpClientRequestContext<MockController.Greeting, MockController.Greeting> request = HttpClientRequestContext.<MockController.Greeting, MockController.Greeting>builder()
                .requestMethod(HttpMethod.POST)
                .path("/")
                .request(new MockController.Greeting("", "Jim", "Server"))
                .responseClass(MockController.Greeting.class)
                .userContext(
                        UserContext.builder().acceptLanguage("en").build()
                ).build();

        verifyResult(client.invoke(request));
    }

    @Test
    void shouldGetPostResponseByReference() {
        HttpClient<MockController.Greeting, MockController.Greeting> client = new HttpClientImpl<>(
                WebClient.builder(),
                "test",
                createConfig()
        );

        HttpClientRequestContext<MockController.Greeting, MockController.Greeting> request = HttpClientRequestContext.<MockController.Greeting, MockController.Greeting>builder()
                .requestMethod(HttpMethod.POST)
                .path("/")
                .request(new MockController.Greeting("", "Jim", "Server"))
                .requestReference(new ParameterizedTypeReference<MockController.Greeting>() {})
                .responseClass(MockController.Greeting.class)
                .userContext(
                        UserContext.builder().acceptLanguage("en").build()
                ).build();

        verifyResult(client.invoke(request));
    }

    @Test
    void shouldGetPostResponseByAllReference() {
        HttpClient<MockController.Greeting, MockController.Greeting> client = new HttpClientImpl<>(
                WebClient.builder(),
                "test",
                createConfig()
        );

        HttpClientRequestContext<MockController.Greeting, MockController.Greeting> request = HttpClientRequestContext.<MockController.Greeting, MockController.Greeting>builder()
                .requestMethod(HttpMethod.POST)
                .path("/")
                .request(new MockController.Greeting("", "Jim", "Server"))
                .requestReference(new ParameterizedTypeReference<>() {})
                .responseReference(new ParameterizedTypeReference<>() {})
                .userContext(
                        UserContext.builder().acceptLanguage("en").build()
                ).build();

        verifyResult(client.invoke(request));
    }

    private void verifyResult(Mono<ResponseEntity<MockController.Greeting>> response) {
        StepVerifier.create(response)
                .consumeNextWith(resp -> {
                    assertThat(resp, notNullValue());
                    assertThat(resp.getBody(), notNullValue());
                    MockController.Greeting greeting = resp.getBody();
                    assertThat(greeting.getFrom(), equalTo("Server"));
                    assertThat(greeting.getTo(), equalTo("Jim"));
                    assertThat(greeting.getGreeting(), equalTo("Hello"));
                }).verifyComplete();
    }

    @Test
    void shouldCreateHttpClient() {

        assertThat(
                new HttpClientImpl<Integer, Integer>(
                        WebClient.builder(),
                        "test",
                        HttpClientConfigTestData.getConfig(this.applicationContext)
                ),
                notNullValue()
        );

        assertThat(
                new HttpClientImpl<Integer, Integer>(
                        WebClient.builder(),
                        "test",
                        HttpClientConfigTestData.getConfig(this.applicationContext),
                        new ObjectMapper()
                ),
                notNullValue()
        );

        assertThat(
                new HttpClientImpl<Integer, Integer>(
                        "test",
                        this.webClient
                ),
                notNullValue()
        );

        assertThat(
                new HttpClientImpl<Integer, Integer>(
                        "test",
                        this.webClient,
                        new ObjectMapper()
                ),
                notNullValue()
        );

        HttpClient<MockController.Greeting, MockController.Greeting> client = new HttpClientImpl<>(
                WebClient.builder(),
                "test",
                createConfig()
        );

        HttpClientRequestContext<MockController.Greeting, MockController.Greeting> request = HttpClientRequestContext.<MockController.Greeting, MockController.Greeting>builder()
                .requestMethod(HttpMethod.POST)
                .path("")
                .request(new MockController.Greeting())
                .requestReference(new ParameterizedTypeReference<>() {})
                .userContext(
                        UserContext.builder().acceptLanguage("en").build()
                ).build();

        assertThrows(IllegalArgumentException.class, () -> client.invoke(request));
    }
}
