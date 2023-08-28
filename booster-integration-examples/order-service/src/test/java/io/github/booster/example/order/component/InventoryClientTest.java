package io.github.booster.example.order.component;

import io.github.booster.example.dto.CheckoutResult;
import io.github.booster.example.dto.Order;
import io.github.booster.example.order.MockUtils;
import io.github.booster.example.order.TestData;
import io.github.booster.factories.TaskFactory;
import io.github.booster.http.client.request.HttpClientRequestContext;
import io.github.booster.task.Task;
import io.github.booster.web.handler.response.WebResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class InventoryClientTest {

    private TaskFactory taskFactory;

    private Task<HttpClientRequestContext<Order, WebResponse<CheckoutResult>>, ResponseEntity<WebResponse<CheckoutResult>>> clientTask;

    @BeforeEach
    void setup() {
        clientTask = MockUtils.mockClientTask();
        taskFactory = MockUtils.mockFactory(clientTask);
    }

    @Test
    void shouldHandleSuccess() {
        InventoryClient client = new InventoryClient(
                this.taskFactory,
                WebClient.builder()
        );
        StepVerifier.create(client.invoke(TestData.createGoodOrder()))
                .consumeNextWith(either -> {
                    assertThat(either.isRight(), equalTo(true));
                    assertThat(either.getOrNull(), notNullValue());

                    CheckoutResult result = either.getOrNull();
                    for (CheckoutResult.Item item: result.getItems()) {
                        assertThat(item.isInStock(), equalTo(true));
                    }
                }).verifyComplete();
    }

    @Test
    void shouldHandleFailure() {
        InventoryClient client = new InventoryClient(
                this.taskFactory,
                WebClient.builder()
        );
        StepVerifier.create(client.invoke(TestData.createBadOrder()))
                .consumeNextWith(either -> {
                    assertThat(either.isRight(), equalTo(true));
                    assertThat(either.getOrNull(), notNullValue());

                    CheckoutResult result = either.getOrNull();
                    for (CheckoutResult.Item item: result.getItems()) {
                        if (item.getProduct().getId().equals("z")) {
                            assertThat(item.isInStock(), equalTo(false));
                        } else {
                            assertThat(item.isInStock(), equalTo(true));
                        }
                    }
                }).verifyComplete();
    }
}
