package io.github.booster.example.order;

import arrow.core.Either;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.booster.commons.circuit.breaker.CircuitBreakerConfig;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.retry.RetryConfig;
import io.github.booster.config.thread.ThreadPoolConfig;
import io.github.booster.config.thread.ThreadPoolSetting;
import io.github.booster.example.dto.CheckoutResult;
import io.github.booster.example.order.component.InventoryClient;
import io.github.booster.example.order.controller.MockController;
import io.github.booster.factories.HttpClientFactory;
import io.github.booster.factories.TaskFactory;
import io.github.booster.http.client.config.HttpClientConnectionConfig;
import io.github.booster.http.client.config.HttpClientConnectionSetting;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                InventoryClient.class,
                MockApplication.class,
                MockController.class
        },
        properties = {
                "booster.http.client.connection.settings.inventory.baseUrl=http://localhost:8082"
        }
)
class MockApplicationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private ApplicationContext applicationContext;

    private InventoryClient inventoryClient;

    @BeforeEach
    void setup() {
        ThreadPoolConfig threadPoolConfig = new ThreadPoolConfig();
        ThreadPoolSetting threadPoolSetting = new ThreadPoolSetting();
        threadPoolSetting.setCoreSize(10);
        threadPoolSetting.setMaxSize(10);
        threadPoolConfig.setSettings(Map.of("inventory", threadPoolSetting));

        HttpClientConnectionConfig httpClientConnectionConfig = new HttpClientConnectionConfig(this.applicationContext);
        HttpClientConnectionSetting httpClientConnectionSetting = new HttpClientConnectionSetting();
        httpClientConnectionSetting.setBaseUrl("http://localhost:" + this.port);
        httpClientConnectionSetting.setConnectionTimeoutMillis(2000);
        httpClientConnectionSetting.setReadTimeoutMillis(2000);

        HttpClientConnectionSetting.ConnectionPoolSetting connectionPool = new HttpClientConnectionSetting.ConnectionPoolSetting();
        connectionPool.setMaxConnections(20);
        httpClientConnectionSetting.setPool(connectionPool);

        httpClientConnectionConfig.setSettings(Map.of("inventory", httpClientConnectionSetting));
        HttpClientFactory httpClientFactory = new HttpClientFactory(
                httpClientConnectionConfig,
                WebClient.builder(),
                this.mapper
        );

        TaskFactory factory = new TaskFactory(
                threadPoolConfig,
                new RetryConfig(),
                new CircuitBreakerConfig(),
                httpClientFactory,
                new MetricsRegistry(this.registry)
        );

        this.inventoryClient = new InventoryClient(factory, WebClient.builder());
    }

    @Test
    void shouldSucceed() {
        assertThat(this.inventoryClient, notNullValue());

        StepVerifier.create(this.inventoryClient.invoke(TestData.createGoodOrder()))
                .consumeNextWith(either -> {
                    assertThat(either, instanceOf(Either.Right.class));
                    CheckoutResult result = either.getOrNull();
                    assertThat(result, notNullValue());
                    assertThat(result.getItems(), hasSize(1));

                    for (CheckoutResult.Item item: result.getItems()) {
                        assertThat(item.isInStock(), is(true));
                    }
                }).verifyComplete();

        StepVerifier.create(this.inventoryClient.invoke(TestData.createBadOrder()))
                .consumeNextWith(either -> {
                    assertThat(either, instanceOf(Either.Right.class));
                    CheckoutResult result = either.orNull();
                    assertThat(result, notNullValue());
                    assertThat(result.getItems(), hasSize(2));

                    for (CheckoutResult.Item item: result.getItems()) {
                        if (item.getProduct().getId().equals("z")) {
                            assertThat(item.isInStock(), is(false));
                        } else {
                            assertThat(item.isInStock(), is(true));
                        }
                    }
                }).verifyComplete();
    }
}
