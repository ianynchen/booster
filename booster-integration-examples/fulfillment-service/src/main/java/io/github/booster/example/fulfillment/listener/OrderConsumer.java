package io.github.booster.example.fulfillment.listener;

import arrow.core.Option;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;
import io.github.booster.commons.circuit.breaker.CircuitBreakerConfig;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.retry.RetryConfig;
import io.github.booster.config.thread.ThreadPoolConfig;
import io.github.booster.messaging.config.GcpPubSubSubscriberConfig;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.processor.gcp.GcpPubSubProcessor;
import io.github.booster.messaging.subscriber.gcp.GcpPubSubPullSubscriber;
import io.github.booster.task.TaskExecutionContext;
import io.github.booster.task.impl.AsyncTask;
import io.github.booster.task.impl.RequestHandlers;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

@Service
public class OrderConsumer {

    private final static Logger log = LoggerFactory.getLogger(OrderConsumer.class);
    public static final String PROCESSOR = "processor";
    public static final String FULFILLMENT = "fulfillment";

    private final GcpPubSubPullSubscriber gcpSubscriber;

    private final GcpPubSubProcessor processor;

    private CountDownLatch latch;

    public OrderConsumer(
            PubSubTemplate template,
            ObjectMapper mapper,
            ThreadPoolConfig threadPoolConfig,
            CircuitBreakerConfig circuitBreakerConfig,
            RetryConfig retryConfig,
            GcpPubSubSubscriberConfig gcpPubSubscriberConfig,
            MeterRegistry registry,
            OpenTelemetryConfig openTelemetryConfig
    ) {
        MetricsRegistry metricsRegistry = new MetricsRegistry(registry);

        this.gcpSubscriber = new GcpPubSubPullSubscriber(
                FULFILLMENT,
                template.getPubSubSubscriberTemplate(),
                threadPoolConfig,
                gcpPubSubscriberConfig,
                metricsRegistry,
                openTelemetryConfig,
                false
        );
        this.processor = new GcpPubSubProcessor(
                this.gcpSubscriber,
                new AsyncTask<>(
                        PROCESSOR,
                        new RequestHandlers<>(
                                Option.fromNullable(null),
                                Option.fromNullable(null)
                        ),
                        new TaskExecutionContext(
                                threadPoolConfig.getOption(PROCESSOR),
                                retryConfig.getOption(PROCESSOR),
                                circuitBreakerConfig.getOption(PROCESSOR),
                                metricsRegistry
                        ),
                        (message) -> {
                            synchronized (this) {
                                if (this.latch != null) {
                                    this.latch.countDown();
                                }
                            }
                            String content = message.getPubsubMessage().getData().toString(StandardCharsets.UTF_8);
                            log.info("fulfillment service - message received, content: [{}]", content);
                            return Mono.just(Option.fromNullable(message));
                        }
                ),
                openTelemetryConfig,
                metricsRegistry,
                true
        );
    }

    @PostConstruct
    void process() {
        this.processor.process().subscribe();
    }

    @PreDestroy
    void destroy() {
        this.gcpSubscriber.stop();
    }

    public void setLatch(CountDownLatch latch) {
        synchronized (this) {
            this.latch = latch;
        }
    }
}
