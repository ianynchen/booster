package io.github.booster.example.order;

import arrow.core.Either;
import arrow.core.EitherKt;
import arrow.core.Option;
import arrow.core.OptionKt;
import io.github.booster.commons.util.EitherUtil;
import io.github.booster.example.dto.CheckoutResult;
import io.github.booster.example.dto.Order;
import io.github.booster.factories.TaskFactory;
import io.github.booster.http.client.request.HttpClientRequestContext;
import io.github.booster.task.Task;
import io.github.booster.web.handler.response.WebResponse;
import kotlin.jvm.functions.Function1;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.just;

public interface MockUtils {

    static TaskFactory mockFactory(
            Task<HttpClientRequestContext<Order, WebResponse<CheckoutResult>>, ResponseEntity<WebResponse<CheckoutResult>>> clientTask
    ) {
        TaskFactory mockFactory = mock(TaskFactory.class);
        when(mockFactory.getHttpTask(anyString(), any(Function1.class))).thenReturn(clientTask);
        return mockFactory;
    }

    static CheckoutResult fromOrder(Order order) {
        CheckoutResult result = new CheckoutResult();
        result.setItems(
                order.getItems().stream().map(lineItem -> {
                    CheckoutResult.Item item = new CheckoutResult.Item();
                    item.setProduct(lineItem.getProduct());
                    if (lineItem.getProduct().getId().equals("z")) {
                        item.setInStock(false);
                    } else {
                        item.setInStock(true);
                    }
                    return item;
                }).collect(Collectors.toList())
        );
        return result;
    }

    static Task mockClientTask() {
        Task task = mock(Task.class);
        when(task.execute(any(HttpClientRequestContext.class))).thenAnswer(invocation -> {
            HttpClientRequestContext<Order, WebResponse<CheckoutResult>> request = invocation.getArgument(0);
            CheckoutResult result = fromOrder(request.getRequest());
            return Mono.just(EitherUtil.convertData(Option.fromNullable(buildResponse(result))));
        });
        return task;
    }

    static ResponseEntity<WebResponse<CheckoutResult>> buildResponse(CheckoutResult checkoutResult) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(
                        WebResponse.<CheckoutResult>builder().response(checkoutResult).build()
                );
    }
}
