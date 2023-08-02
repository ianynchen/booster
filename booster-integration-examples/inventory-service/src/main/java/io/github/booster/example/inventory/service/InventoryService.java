package io.github.booster.example.inventory.service;

import arrow.core.Either;
import arrow.core.Option;
import io.github.booster.commons.circuit.breaker.CircuitBreakerConfig;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.retry.RetryConfig;
import io.github.booster.config.thread.ThreadPoolConfig;
import io.github.booster.example.dto.CheckoutResult;
import io.github.booster.example.dto.LineItem;
import io.github.booster.example.dto.Order;
import io.github.booster.task.Task;
import io.github.booster.task.TaskExecutionContext;
import io.github.booster.task.impl.AsyncTask;
import io.github.booster.task.impl.RequestHandlers;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    public static final String REFILL = "refill";
    public static final String CHECKOUT = "checkout";

    private final Map<String, Integer> inventory;

    private final Task<Order, Map<String, Integer>> refillTask;

    private final Task<Order, CheckoutResult> checkoutTask;

    private final MetricsRegistry registry;

    @Autowired
    public InventoryService(
            ThreadPoolConfig threadPoolConfig,
            RetryConfig retryConfig,
            CircuitBreakerConfig circuitBreakerConfig,
            MeterRegistry registry
    ) {
        this.inventory = new HashMap<>();
        this.reset();

        this.registry = new MetricsRegistry(registry);
        this.refillTask = new AsyncTask<>(
                REFILL,
                new RequestHandlers<>(
                        Option.fromNullable(null),
                        Option.fromNullable(null)
                ),
                new TaskExecutionContext(
                        threadPoolConfig.getOption(REFILL),
                        retryConfig.getOption(REFILL),
                        circuitBreakerConfig.getOption(REFILL),
                        new MetricsRegistry(registry)
                ),
                (order) -> {
                    log.info("inventory service - inside refill task");
                    this.reset();
                    return Mono.just(Option.fromNullable(this.copy()));
                }
        );

        this.checkoutTask = new AsyncTask<>(
                CHECKOUT,
                new RequestHandlers<>(
                        Option.fromNullable(null),
                        Option.fromNullable(null)
                ),
                new TaskExecutionContext(
                        threadPoolConfig.getOption(CHECKOUT),
                        retryConfig.getOption(CHECKOUT),
                        circuitBreakerConfig.getOption(CHECKOUT),
                        new MetricsRegistry(registry)
                ),
                (order) -> Mono.just(Option.fromNullable(this.checkInventory(order)))
        );
    }

    private void reset() {
        log.info("reset inventory and refill all stock to 100");
        synchronized (this.inventory) {
            this.inventory.clear();
            for (char i = 'a'; i <= 'z'; i++) {
                this.inventory.put(String.valueOf(i), 100);
            }
        }
    }

    private Map<String, Integer> copy() {
        log.info("make a copy of current inventory snapshot");
        synchronized (this.inventory) {
            return new HashMap<>(this.inventory);
        }
    }

    private CheckoutResult checkInventory(Order order) {

        log.info("checking inventory for order: {}", order);

        CheckoutResult result = new CheckoutResult();
        result.setItems(new ArrayList<>());

        synchronized (this.inventory) {
            for (LineItem item : order.getItems()) {
                if (this.inventory.containsKey(item.getProduct().getId())) {
                    Integer quantity = this.inventory.get(item.getProduct().getId());
                    if (quantity >= item.getQuantity()) {
                        CheckoutResult.Item itemResult = new CheckoutResult.Item();
                        itemResult.setProduct(item.getProduct());
                        itemResult.setInStock(true);
                        this.inventory.put(
                                item.getProduct().getId(),
                                this.inventory.get(item.getProduct().getId()) - item.getQuantity()
                        );
                        result.getItems().add(itemResult);
                    } else {
                        CheckoutResult.Item itemResult = new CheckoutResult.Item();
                        itemResult.setProduct(item.getProduct());
                        itemResult.setInStock(false);
                        result.getItems().add(itemResult);
                    }
                } else {
                    CheckoutResult.Item itemResult = new CheckoutResult.Item();
                    itemResult.setProduct(item.getProduct());
                    itemResult.setInStock(false);
                    result.getItems().add(itemResult);
                }
            }
        }
        log.info("inventory check result: {}", result);
        log.info("resulting inventory: {}", this.copy());
        return result;
    }

    public Mono<Either<Throwable, Option<CheckoutResult>>> checkout(Order order) {
        return this.checkoutTask.execute(order);
    }

    public Mono<Either<Throwable, Option<Map<String, Integer>>>> refill() {
        return this.refillTask.execute(new Order());
    }
}
