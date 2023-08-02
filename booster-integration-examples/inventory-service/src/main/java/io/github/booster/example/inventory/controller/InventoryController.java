package io.github.booster.example.inventory.controller;

import arrow.core.Either;
import arrow.core.Option;
import io.github.booster.example.dto.CheckoutResult;
import io.github.booster.example.dto.Order;
import io.github.booster.example.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@EnableAutoConfiguration
@RequestMapping("/api/v1")
public class InventoryController {

    private static final Logger log = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService service;

    @Autowired
    public InventoryController(
            InventoryService service
    ) {
        this.service = service;
    }

    @PostMapping("/inventory/order")
    public Mono<Either<Throwable, Option<CheckoutResult>>> checkout(
            @RequestBody
            Order order
    ) {
        log.info("inventory service - checkout request, order content: [{}]", order);
        return this.service.checkout(order);
    }

    @PostMapping("/inventory/stock")
    public Mono<Either<Throwable, Option<Map<String, Integer>>>> refill() {
        log.info("inventory service - refill request, refilling inventory");
        return this.service.refill();
    }
}
