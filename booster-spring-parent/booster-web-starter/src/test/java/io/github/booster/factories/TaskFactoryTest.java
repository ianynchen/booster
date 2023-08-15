package io.github.booster.factories;

import arrow.core.Either;
import arrow.core.EitherKt;
import arrow.core.Option;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.booster.commons.circuit.breaker.CircuitBreakerConfig;
import io.github.booster.commons.circuit.breaker.CircuitBreakerSetting;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.retry.RetryConfig;
import io.github.booster.commons.retry.RetrySetting;
import io.github.booster.commons.util.EitherUtil;
import io.github.booster.config.BoosterConfig;
import io.github.booster.config.example.BoosterSampleApplication;
import io.github.booster.config.example.dto.GreetingResponse;
import io.github.booster.config.thread.ThreadPoolConfig;
import io.github.booster.config.thread.ThreadPoolSetting;
import io.github.booster.http.client.HttpClient;
import io.github.booster.http.client.config.HttpClientConnectionConfig;
import io.github.booster.http.client.config.HttpClientConnectionSetting;
import io.github.booster.http.client.request.HttpClientRequestContext;
import io.github.booster.task.Task;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {BoosterSampleApplication.class, BoosterConfig.class})
class TaskFactoryTest {

    @Autowired
    private HttpClientConnectionConfig httpClientConnectionConfig;

    private ThreadPoolConfig threadPoolConfig;

    private RetryConfig retryConfig;

    private CircuitBreakerConfig circuitBreakerConfig;

    private final ObjectMapper mapper = new ObjectMapper();

    private final MetricsRegistry registry = new MetricsRegistry(new SimpleMeterRegistry());

    private TaskFactory factory;

    private TaskFactory factoryWithMock;

    @Mock
    private HttpClientFactory mockHttpClientFactory;

    private HttpClient<GreetingResponse, GreetingResponse> mockHttpClient;

    @BeforeEach
    void setup() {
        HttpClientConnectionSetting setting = new HttpClientConnectionSetting();
        setting.setBaseUrl("http://www.ibm.com");
        this.httpClientConnectionConfig.setSettings(Map.of("client", setting));

        this.threadPoolConfig = new ThreadPoolConfig(null, this.registry);
        this.threadPoolConfig.setSettings(
                Map.of(
                        "async", new ThreadPoolSetting(),
                        "sync", new ThreadPoolSetting(),
                        "client", new ThreadPoolSetting()
                )
        );

        this.retryConfig = new RetryConfig();
        this.retryConfig.setSettings(
                Map.of("client", new RetrySetting())
        );

        this.circuitBreakerConfig = new CircuitBreakerConfig();
        this.circuitBreakerConfig.setSettings(
                Map.of("client", new CircuitBreakerSetting())
        );

        this.factory = new TaskFactory(
                this.threadPoolConfig,
                this.retryConfig,
                this.circuitBreakerConfig,
                new HttpClientFactory(this.httpClientConnectionConfig, WebClient.builder(), this.mapper),
                this.registry
        );
    }

    @Test
    void shouldCreateAsyncTask() {
        assertThat(this.factory, notNullValue());

        Task<String, Integer> task = this.factory.getAsyncTask(
                "async",
                str -> Mono.just(Option.fromNullable(str.length()))
        );

        Task<String, Integer> task2 = this.factory.getAsyncTask(
                "async2",
                str -> Mono.just(Option.fromNullable(str.length()))
        );

        Task<String, Integer> task3 = this.factory.getAsyncTask(
                "async",
                str -> Mono.just(Option.fromNullable(str.length()))
        );

        assertThat(task, notNullValue());
        assertThat(task2, notNullValue());
        assertThat(task3, notNullValue());
        assertThat(task, sameInstance(task3));
        assertThat(task, not(sameInstance(task2)));

        StepVerifier.create(task.execute("hello"))
                .consumeNextWith(either -> {
                    assertThat(either.isRight(), is(true));

                    Option<Integer> value = EitherKt.getOrElse(either, o -> Option.fromNullable(0));
                    assertThat(value.orNull(), is("hello".length()));
                }).verifyComplete();
    }

    @Test
    void shouldCreateSyncTask() {
        assertThat(this.factory, notNullValue());

        Task<String, Integer> task = this.factory.getSyncTask(
                "async",
                str -> Option.fromNullable(str.length())
        );

        Task<String, Integer> task2 = this.factory.getSyncTask(
                "async2",
                str -> Option.fromNullable(str.length())
        );

        Task<String, Integer> task3 = this.factory.getSyncTask(
                "async",
                str -> Option.fromNullable(str.length())
        );

        assertThat(task, notNullValue());
        assertThat(task2, notNullValue());
        assertThat(task3, notNullValue());
        assertThat(task, sameInstance(task3));
        assertThat(task, not(sameInstance(task2)));

        StepVerifier.create(task.execute("hello"))
                .consumeNextWith(either -> {
                    assertThat(either.isRight(), is(true));

                    Option<Integer> value = EitherKt.getOrElse(either, o -> Option.fromNullable(0));
                    assertThat(value.orNull(), is("hello".length()));
                }).verifyComplete();
    }

    @Test
    void shouldReturnResponse() {

        this.mockHttpClient = mock(HttpClient.class);
        when(this.mockHttpClient.invoke(any()))
                .thenReturn(
                        Mono.just(
                                ResponseEntity.ok(
                                        GreetingResponse.builder()
                                                .greeting("hola")
                                                .from("server")
                                                .build()
                                )
                        )
                );

        this.mockHttpClientFactory = mock(HttpClientFactory.class);
        when(this.mockHttpClientFactory.get(anyString())).thenReturn(this.mockHttpClient);

        this.factoryWithMock = new TaskFactory(
                this.threadPoolConfig,
                this.retryConfig,
                this.circuitBreakerConfig,
                this.mockHttpClientFactory,
                this.registry
        );

        Task<HttpClientRequestContext<GreetingResponse, GreetingResponse>, ResponseEntity<GreetingResponse>> task =
                this.factoryWithMock.getHttpTask("test");
        assertThat(task, notNullValue());

        HttpClientRequestContext<GreetingResponse, GreetingResponse> context =
                HttpClientRequestContext.<GreetingResponse, GreetingResponse>builder()
                        .request(
                                GreetingResponse.builder().greeting("hello").from("world").build()
                        ).responseClass(GreetingResponse.class)
                        .path("")
                        .requestMethod(HttpMethod.GET)
                        .build();
        StepVerifier.create(task.execute(context))
                .consumeNextWith(either -> {
                    assertThat(either.isRight(), is(true));
                    GreetingResponse response = either.getOrNull().orNull().getBody();
                    assertThat(response, notNullValue());
                    assertThat(response.getFrom(), is("server"));
                    assertThat(response.getGreeting(), is("hola"));
                }).verifyComplete();
    }

    @Test
    void shouldCreateHttpClientTask() {
        assertThat(this.factory, notNullValue());

        Task<HttpClientRequestContext<Object, GreetingResponse>, ResponseEntity<GreetingResponse>> task =
                this.factory.getHttpTask("client");
        assertThat(task, notNullValue());

        assertThrows(
                IllegalArgumentException.class,
                () -> this.factory.getHttpTask("client2")
        );

        assertThat(this.factory.getHttpTask("client"), sameInstance(task));

        Either<Throwable, Option<HttpClientRequestContext<Object, GreetingResponse>>> request =
                EitherUtil.convertThrowable(new IllegalArgumentException());
        StepVerifier.create(
                task.execute(request)
        ).consumeNextWith(either -> {
            assertThat(either.isRight(), is(true));
            ResponseEntity<?> response = either.getOrNull().orNull();
            assertThat(response, notNullValue());
            assertThat(response.getStatusCode(), equalTo(HttpStatus.PRECONDITION_FAILED));
        }).verifyComplete();
    }
}
