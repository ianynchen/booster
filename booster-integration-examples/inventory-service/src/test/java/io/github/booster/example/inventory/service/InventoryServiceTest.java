package io.github.booster.example.inventory.service;

import io.github.booster.example.dto.CheckoutResult;
import io.github.booster.example.dto.LineItem;
import io.github.booster.example.dto.Order;
import io.github.booster.example.dto.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest
class InventoryServiceTest {

    @Autowired
    private InventoryService service;

    private Order createOrder(int quantity) {
        Order order = new Order();
        order.setItems(new ArrayList<>());

        Product product = new Product("a", "a", 1.2);
        LineItem item = new LineItem(product, quantity);

        order.getItems().add(item);
        return order;
    }

    @Test
    void shouldCheckoutAndRefill() {

        // should not be able to checkout 110 items with inventory of 100
        StepVerifier.create(this.service.checkout(this.createOrder(110)))
                .consumeNextWith(either -> {
                    assertThat(either.isRight(), equalTo(true));
                    assertThat(either.getOrNull(), notNullValue());
                    CheckoutResult result = either.getOrNull().orNull();
                    assertThat(result, notNullValue());
                    assertThat(result.getItems(), hasSize(1));
                    CheckoutResult.Item item = result.getItems().get(0);
                    assertThat(item.getProduct().getId(), equalTo("a"));
                    assertThat(item.isInStock(), equalTo(false));
                }).verifyComplete();

        // should be able to checkout 3 items with inventory of 100
        StepVerifier.create(this.service.checkout(this.createOrder(3)))
                .consumeNextWith(either -> {
                    assertThat(either.isRight(), equalTo(true));
                    assertThat(either.getOrNull(), notNullValue());
                    CheckoutResult result = either.getOrNull().orNull();
                    assertThat(result, notNullValue());
                    assertThat(result.getItems(), hasSize(1));
                    CheckoutResult.Item item = result.getItems().get(0);
                    assertThat(item.getProduct().getId(), equalTo("a"));
                    assertThat(item.isInStock(), equalTo(true));
                }).verifyComplete();

        // should not be able to checkout 100 items with inventory of 97
        StepVerifier.create(this.service.checkout(this.createOrder(100)))
                .consumeNextWith(either -> {
                    assertThat(either.isRight(), equalTo(true));
                    assertThat(either.getOrNull(), notNullValue());
                    CheckoutResult result = either.getOrNull().orNull();
                    assertThat(result, notNullValue());
                    assertThat(result.getItems(), hasSize(1));
                    CheckoutResult.Item item = result.getItems().get(0);
                    assertThat(item.getProduct().getId(), equalTo("a"));
                    assertThat(item.isInStock(), equalTo(false));
                }).verifyComplete();

        // should restore inventory to 100
        StepVerifier.create(this.service.refill())
                .consumeNextWith(either -> {
                    assertThat(either.isRight(), equalTo(true));
                    assertThat(either.getOrNull(), notNullValue());
                    Map<String, Integer> inventory = either.getOrNull().orNull();
                    assertThat(inventory, notNullValue());
                    assertThat(inventory.entrySet(), hasSize(26));
                    assertThat(inventory.get("a"), equalTo(100));
                }).verifyComplete();

        // should be able to checkout 100 items with inventory of 100
        StepVerifier.create(this.service.checkout(this.createOrder(100)))
                .consumeNextWith(either -> {
                    assertThat(either.isRight(), equalTo(true));
                    assertThat(either.getOrNull(), notNullValue());
                    CheckoutResult result = either.getOrNull().orNull();
                    assertThat(result, notNullValue());
                    assertThat(result.getItems(), hasSize(1));
                    CheckoutResult.Item item = result.getItems().get(0);
                    assertThat(item.getProduct().getId(), equalTo("a"));
                    assertThat(item.isInStock(), equalTo(true));
                }).verifyComplete();
    }
}
