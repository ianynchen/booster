package io.github.booster.example.order.service;

import arrow.core.Either;
import arrow.core.EitherKt;
import arrow.core.Option;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.util.EitherUtil;
import io.github.booster.config.thread.ThreadPoolConfig;
import io.github.booster.example.dto.CheckoutResult;
import io.github.booster.example.dto.Order;
import io.github.booster.example.dto.OrderResult;
import io.github.booster.example.order.component.InventoryClient;
import io.github.booster.example.order.config.GcpPubsubConfig;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.publisher.PublisherRecord;
import io.github.booster.messaging.publisher.gcp.GcpPubSubPublisher;
import io.github.booster.messaging.publisher.gcp.PubsubRecord;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class OrderService {

    public static final String FULFILLMENT = "fulfillment";

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    public static final String ORDER = "order";

    private final GcpPubSubPublisher<String> publisher;

    private final GcpPubsubConfig gcpPubsubConfig;

    private final ObjectMapper mapper;

    private final InventoryClient inventoryClient;

    public OrderService(
            PubSubTemplate template,
            InventoryClient inventoryClient,
            GcpPubsubConfig gcpPubsubConfig,
            ThreadPoolConfig threadPoolConfig,
            ObjectMapper mapper,
            MeterRegistry registry,
            OpenTelemetryConfig openTelemetryConfig
    ) {
        MetricsRegistry metricsRegistry = new MetricsRegistry(registry);
        this.gcpPubsubConfig = gcpPubsubConfig;
        this.mapper = mapper;
        this.publisher = new GcpPubSubPublisher<>(
                ORDER,
                template.getPubSubPublisherTemplate(),
                threadPoolConfig,
                metricsRegistry,
                openTelemetryConfig,
                true
        );
        this.inventoryClient = inventoryClient;
    }

    public Mono<Either<Throwable, Option<OrderResult>>> checkout(Order order) {
        return inventoryClient.invoke(order)
                .flatMap(either -> EitherKt.getOrElse(
                        either.map(checkoutResult -> this.publishToPubSub(order, checkoutResult)),
                        t -> {
                            log.error("order service - previous error encountered invoking inventory", t);
                            return Mono.just(EitherUtil.convertThrowable(t));
                        }
                ));
    }

    private Either<Throwable, Option<OrderResult>> convertPublisherRecord(
            Order order,
            Either<Throwable, Option<PublisherRecord>> maybeRecord
    ) {
        return EitherKt.getOrElse(
                maybeRecord.map(publisherRecordOption -> {
                    log.info("order service - publish to gcp pub/sub is right value");
                    PublisherRecord publisherRecord = publisherRecordOption.orNull();
                    log.info("order service - publisher record [{}]", publisherRecord);
                    return EitherUtil.convertData(
                            Option.fromNullable(
                                    new OrderResult(order, true))
                            );
                }),
                t -> {
                    log.error("order service - publish to gcp pub/sub is left value", t);
                    return EitherUtil.convertData(
                            Option.fromNullable(
                                    new OrderResult(order, false))
                            );
                }
        );
    }

    private Mono<Either<Throwable, Option<OrderResult>>> publishToPubSub(
            Order order,
            CheckoutResult checkoutResult
    ) {
        if (checkoutResult == null || checkoutResult.getItems() == null) {
            log.error("order service - unknown error, no checkout result or items available, order: [{}], checkout result: [{}]", order, checkoutResult);
            return Mono.just(
                    EitherUtil.convertData(
                            Option.fromNullable(
                                    new OrderResult(order, false)
                            )
                    )
            );
        }

        boolean allInStock = checkoutResult.getItems().stream().allMatch(CheckoutResult.Item::isInStock);
        log.info("order service - order: [{}] all in stock: [{}], checkout result: [{}]", order, allInStock, checkoutResult);

        if (!allInStock) {
            log.warn("order service - not all items in stock, return response without sending to pub/sub");
            return Mono.just(
                    EitherUtil.convertData(
                            Option.fromNullable(
                                    new OrderResult(order, false)
                            )
                    )
            );
        }

        try {
            String payload = this.mapper.writeValueAsString(order);

            log.info("order service - all in stock, sending to pub/sub");
            return this.publisher.publish(
                    this.gcpPubsubConfig.getTopic(),
                    new PubsubRecord<>(payload)
            ).map(maybePublisherRecord -> this.convertPublisherRecord(order, maybePublisherRecord));
        } catch (JsonProcessingException e) {
            log.error("order service - json serialization error", e);
            return Mono.just(
                    EitherUtil.convertData(
                            Option.fromNullable(
                                    new OrderResult(order, false)
                            )
                    )
            );
        }
    }
}
