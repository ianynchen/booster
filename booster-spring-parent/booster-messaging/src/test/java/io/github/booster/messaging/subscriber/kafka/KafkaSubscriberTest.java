package io.github.booster.messaging.subscriber.kafka;

import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.config.thread.ThreadPoolConfig;
import io.github.booster.config.thread.ThreadPoolSetting;
import io.github.booster.messaging.config.KafkaSubscriberConfig;
import io.github.booster.messaging.config.KafkaSubscriberSetting;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class KafkaSubscriberTest {

    private ThreadPoolConfig config = new ThreadPoolConfig();

    private MetricsRegistry registry = new MetricsRegistry(new SimpleMeterRegistry());

    private KafkaSubscriberConfig kafkaSubscriberConfig = new KafkaSubscriberConfig();

    @BeforeEach
    private void setup() {
        ThreadPoolSetting setting = new ThreadPoolSetting();
        setting.setCoreSize(2);
        this.config.setSettings(Map.of("test", setting));
        this.kafkaSubscriberConfig.setSettings(Map.of("test", new KafkaSubscriberSetting()));
    }

    @Test
    void shouldCreate() {
        assertThat(
                new KafkaSubscriber<>(
                        "test",
                        this.config,
                        this.kafkaSubscriberConfig,
                        this.registry
                ),
                notNullValue()
        );
        assertThat(
                new KafkaSubscriber<>(
                        "test",
                        this.config,
                        this.kafkaSubscriberConfig,
                        null
                ),
                notNullValue()
        );
    }

    @Test
    void shouldFailCreate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaSubscriber<>(
                        null,
                        this.config,
                        this.kafkaSubscriberConfig,
                        this.registry
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaSubscriber<>(
                        "",
                        this.config,
                        this.kafkaSubscriberConfig,
                        this.registry
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaSubscriber<>(
                        " ",
                        this.config,
                        this.kafkaSubscriberConfig,
                        this.registry
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaSubscriber<>(
                        "abc",
                        this.config,
                        this.kafkaSubscriberConfig,
                        this.registry
                )
        );
        this.config.setSettings(Map.of("abc", new ThreadPoolSetting()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaSubscriber<>(
                        "abc",
                        this.config,
                        this.kafkaSubscriberConfig,
                        this.registry
                )
        );

        this.config.setSettings(Map.of("test", new ThreadPoolSetting()));
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaSubscriber<>(
                        "test",
                        null,
                        this.kafkaSubscriberConfig,
                        this.registry
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaSubscriber<>(
                        "test",
                        this.config,
                        null,
                        this.registry
                )
        );
    }

    private Acknowledgment createAcknowledgment() {
        return mock(Acknowledgment.class);
    }

    @Test
    void shouldReceive() {
        KafkaSubscriber<Integer> subscriber = new KafkaSubscriber<>(
                "test",
                this.config,
                this.kafkaSubscriberConfig,
                this.registry
        );

        assertThat(subscriber.getName(), is("test"));

        Thread runnable = new Thread() {

            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    subscriber.push(i, createAcknowledgment());
                }
            }
        };
        runnable.start();

        Flux<SubscriberRecord<Integer>> flux = subscriber.flatFlux();
        StepVerifier.create(flux.take(5))
                .consumeNextWith(record -> assertThat(record.getData(), equalTo(0)))
                .consumeNextWith(record -> assertThat(record.getData(), equalTo(1)))
                .consumeNextWith(record -> assertThat(record.getData(), equalTo(2)))
                .consumeNextWith(record -> assertThat(record.getData(), equalTo(3)))
                .consumeNextWith(record -> assertThat(record.getData(), equalTo(4)))
                .verifyComplete();

        subscriber.stop();
    }
}
