package io.github.booster.example.order.service;

import arrow.core.Option;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.core.publisher.PubSubPublisherTemplate;
import com.google.cloud.spring.pubsub.support.converter.PubSubMessageConverter;
import com.google.pubsub.v1.PubsubMessage;
import io.github.booster.commons.util.EitherUtil;
import io.github.booster.config.thread.ThreadPoolConfig;
import io.github.booster.config.thread.ThreadPoolSetting;
import io.github.booster.example.dto.Order;
import io.github.booster.example.dto.OrderResult;
import io.github.booster.example.order.MockUtils;
import io.github.booster.example.order.TestData;
import io.github.booster.example.order.component.InventoryClient;
import io.github.booster.example.order.config.GcpPubsubConfig;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.AsyncResult;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private OrderService orderService;

    @BeforeEach
    void setup() {

        PubSubMessageConverter converter = mock(PubSubMessageConverter.class);
        when(converter.toPubSubMessage(any(), any(Map.class)))
                .thenAnswer(invocation -> mock(PubsubMessage.class));

        PubSubPublisherTemplate publisherTemplate = mock(PubSubPublisherTemplate.class);
        when(publisherTemplate.publish(anyString(), any(PubsubMessage.class)))
                .thenAnswer(invocation -> new AsyncResult<>("test"));
        when(publisherTemplate.getMessageConverter()).thenReturn(converter);

        PubSubTemplate pubSubTemplate = mock(PubSubTemplate.class);
        when(pubSubTemplate.getPubSubPublisherTemplate())
                .thenReturn(publisherTemplate);

        InventoryClient inventoryClient = mock(InventoryClient.class);
        when(inventoryClient.invoke(any(Order.class)))
                .thenAnswer(invocation -> Mono.just(
                        EitherUtil.convertData(
                                MockUtils.fromOrder(invocation.getArgument(0))
                        )
                ));

        GcpPubsubConfig gcpPubsubConfig = new GcpPubsubConfig(
                "abc",
                "abc",
                "abc",
                "abc",
                false
        );

        ThreadPoolConfig threadPoolConfig = new ThreadPoolConfig(null, null);
        threadPoolConfig.setSettings(Map.of("order", new ThreadPoolSetting()));

        this.orderService = new OrderService(
                pubSubTemplate,
                inventoryClient,
                gcpPubsubConfig,
                threadPoolConfig,
                new ObjectMapper(),
                new SimpleMeterRegistry(),
                new OpenTelemetryConfig(null, "order")
        );
    }

    @Test
    void shouldHandleGoodOrder() {
        StepVerifier.create(this.orderService.checkout(TestData.createGoodOrder()))
                .consumeNextWith(either -> {
                    assertThat(either.isRight(), equalTo(true));
                    Option<OrderResult> option = either.getOrNull();
                    assertThat(option, notNullValue());
                    assertThat(option.isDefined(), equalTo(true));
                    assertThat(option.orNull(), notNullValue());

                    OrderResult result = option.orNull();
                    assertThat(result.isSuccess(), equalTo(true));
                }).verifyComplete();
    }

    @Test
    void shouldHandleBadOrder() {
        StepVerifier.create(this.orderService.checkout(TestData.createBadOrder()))
                .consumeNextWith(either -> {
                    assertThat(either.isRight(), equalTo(true));
                    Option<OrderResult> option = either.getOrNull();
                    assertThat(option, notNullValue());
                    assertThat(option.isDefined(), equalTo(true));
                    assertThat(option.orNull(), notNullValue());

                    OrderResult result = option.orNull();
                    assertThat(result.isSuccess(), equalTo(false));
                }).verifyComplete();
    }
}
