package io.github.booster.example.order.controller;

import arrow.core.Either;
import io.github.booster.commons.util.EitherUtil;
import io.github.booster.example.dto.CheckoutResult;
import io.github.booster.example.dto.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class MockController {

    private static final Logger log = LoggerFactory.getLogger(MockController.class);

    @PostMapping("/inventory/order")
    public Mono<Either<Throwable, CheckoutResult>> checkout(
            @RequestBody
            Order order
    ) {
        log.info("inventory service - checkout request, order content: [{}]", order);
        if (order == null || CollectionUtils.isEmpty(order.getItems())) {
            return Mono.just(EitherUtil.convertData(new CheckoutResult()));
        } else {
            List<CheckoutResult.Item> items = order.getItems()
                    .stream()
                    .map(lineItem -> {
                        CheckoutResult.Item item = new CheckoutResult.Item();
                        item.setProduct(lineItem.getProduct());

                        if (lineItem.getProduct().getId().equals("z")) {
                            item.setInStock(false);
                        } else {
                            item.setInStock(true);
                        }
                        return item;
                    }).collect(Collectors.toList());
            CheckoutResult checkoutResult = new CheckoutResult();
            checkoutResult.setItems(items);
            return Mono.just(EitherUtil.convertData(checkoutResult));
        }
    }
}
