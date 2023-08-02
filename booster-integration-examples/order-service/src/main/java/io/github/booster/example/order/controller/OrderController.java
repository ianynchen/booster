package io.github.booster.example.order.controller;

import arrow.core.Either;
import arrow.core.Option;
import io.github.booster.example.dto.Order;
import io.github.booster.example.dto.OrderResult;
import io.github.booster.example.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
public class OrderController {

    private final static Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;

    @Autowired
    public OrderController(
            OrderService orderService
    ) {
        this.orderService = orderService;
    }

    @PostMapping("/order")
    public Mono<Either<Throwable, Option<OrderResult>>> checkout(
            @RequestBody
            Order order
    ) {
        log.info("order service - request received, order [{}]", order);
        return this.orderService.checkout(order);
    }
}
