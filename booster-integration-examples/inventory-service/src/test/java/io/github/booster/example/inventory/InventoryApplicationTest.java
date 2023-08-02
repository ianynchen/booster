package io.github.booster.example.inventory;

import io.github.booster.example.dto.CheckoutResult;
import io.github.booster.example.dto.LineItem;
import io.github.booster.example.dto.Order;
import io.github.booster.example.dto.Product;
import io.github.booster.web.handler.response.WebResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryApplicationTest {

    @Autowired
    private WebTestClient client;

    @Test
    void shouldRefill() {
        this.client.post()
                .uri("/api/v1/inventory/stock")
                .contentLength(0L)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(new ParameterizedTypeReference<WebResponse<Map>>() {});
    }

    @Test
    void shouldCheckout() {
        Order order = new Order();
        order.setItems(new ArrayList<>());

        Product product = new Product("a", "a", 1.2);
        LineItem item = new LineItem(product, 3);

        order.getItems().add(item);

        this.client.post()
                .uri("/api/v1/inventory/order")
                .bodyValue(order)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(new ParameterizedTypeReference<WebResponse<CheckoutResult>>() {});
    }
}
