package io.github.booster.example.order.component;

import arrow.core.Either;
import arrow.core.EitherKt;
import arrow.core.Option;
import io.github.booster.commons.util.EitherUtil;
import io.github.booster.example.dto.CheckoutResult;
import io.github.booster.example.dto.Order;
import io.github.booster.factories.TaskFactory;
import io.github.booster.http.client.request.HttpClientRequestContext;
import io.github.booster.task.Task;
import io.github.booster.web.handler.response.WebResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Profile("!client_mock_test")
@Component
public class InventoryClient {

    public static final String INVENTORY = "inventory";

    public static final String API_V_1_INVENTORY_ORDER = "/api/v1/inventory/order";

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    private final Task<HttpClientRequestContext<Order, WebResponse<CheckoutResult>>, ResponseEntity<WebResponse<CheckoutResult>>> clientTask;

    public InventoryClient(
            TaskFactory taskFactory,
            WebClient.Builder webClientBuilder
    ) {
        this.clientTask = taskFactory.getHttpTask(
                INVENTORY,
                t -> Option.fromNullable(
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
                )
        );
    }

    public Mono<Either<Throwable, CheckoutResult>> invoke(Order order) {
        HttpClientRequestContext<Order, WebResponse<CheckoutResult>> request = HttpClientRequestContext.<Order, WebResponse<CheckoutResult>>builder()
                .request(order)
                .requestMethod(HttpMethod.POST)
                .path(API_V_1_INVENTORY_ORDER)
                .responseReference(new ParameterizedTypeReference<WebResponse<CheckoutResult>>() {
                }).build();
        return this.clientTask.execute(request)
                .flatMap(either -> EitherKt.getOrElse(
                        either.map(option -> {
                            ResponseEntity<WebResponse<CheckoutResult>> response = option.orNull();
                            CheckoutResult checkoutResult = response == null ?
                                    null :
                                    response.getBody() == null ?
                                            null :
                                            response.getBody().getResponse();
                            log.info("inventory client - checkout result from inventory service: [{}]", checkoutResult);
                            return Mono.just(EitherUtil.convertData(checkoutResult));
                        }),
                        (t) -> {
                            log.error("inventory client - previous error encountered invoking inventory", t);
                            return Mono.just(EitherUtil.convertThrowable(t));
                        }
                ));
    }
}
