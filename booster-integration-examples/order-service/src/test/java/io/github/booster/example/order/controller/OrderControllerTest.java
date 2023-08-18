package io.github.booster.example.order.controller;

import arrow.core.Option;
import io.github.booster.commons.util.EitherUtil;
import io.github.booster.example.dto.CheckoutResult;
import io.github.booster.example.dto.Order;
import io.github.booster.example.dto.OrderResult;
import io.github.booster.example.order.MockUtils;
import io.github.booster.example.order.TestData;
import io.github.booster.example.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderControllerTest {

    private OrderController controller;

    @BeforeEach
    void setup() {
        OrderService orderService = mock(OrderService.class);
        when(orderService.checkout(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    boolean inStock = MockUtils.fromOrder(order)
                            .getItems()
                            .stream()
                            .allMatch(CheckoutResult.Item::isInStock);
                    return Mono.just(
                            EitherUtil.convertData(
                                    Option.fromNullable(
                                            new OrderResult(order, inStock)
                                    )
                            )
                    );
                });
        this.controller = new OrderController(orderService);
    }

    @Test
    void shouldHandleGoodOrder() {
        StepVerifier.create(this.controller.checkout(TestData.createGoodOrder()))
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
        StepVerifier.create(this.controller.checkout(TestData.createBadOrder()))
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
