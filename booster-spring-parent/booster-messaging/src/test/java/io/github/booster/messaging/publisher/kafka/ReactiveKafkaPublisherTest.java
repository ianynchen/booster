package io.github.booster.messaging.publisher.kafka;

import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.config.thread.ThreadPoolConfigGeneric;
import io.github.booster.config.thread.ThreadPoolSetting;
import io.github.booster.messaging.Greeting;
import io.github.booster.messaging.publisher.PublisherRecord;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderResult;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReactiveKafkaPublisherTest {

    private KafkaSender<String, Greeting> sender;

    private ThreadPoolConfigGeneric threadPoolConfig;

    @BeforeEach
    void setup() {
        this.sender = mock(KafkaSender.class);

        this.threadPoolConfig = new ThreadPoolConfigGeneric();
        this.threadPoolConfig.setSettings(
                Map.of("test", new ThreadPoolSetting())
        );
    }

    @Test
    void shouldFailCreate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ReactiveKafkaPublisher<>(
                        null,
                        this.sender,
                        this.threadPoolConfig,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new ReactiveKafkaPublisher<>(
                        "",
                        this.sender,
                        this.threadPoolConfig,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new ReactiveKafkaPublisher<>(
                        " ",
                        this.sender,
                        this.threadPoolConfig,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new ReactiveKafkaPublisher<Greeting>(
                        "abc",
                        null,
                        this.threadPoolConfig,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new ReactiveKafkaPublisher<>(
                        "abc",
                        this.sender,
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new ReactiveKafkaPublisher<>(
                        "abc",
                        this.sender,
                        this.threadPoolConfig,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );
    }

    @Test
    void shouldCreate() {
        assertThat(
                new ReactiveKafkaPublisher<>(
                        "test",
                        this.sender,
                        this.threadPoolConfig,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                ),
                notNullValue()
        );
        assertThat(
                new ReactiveKafkaPublisher<>(
                        "test",
                        this.sender,
                        this.threadPoolConfig,
                        null,
                        null,
                        false
                ),
                notNullValue()
        );
    }

    @Test
    void shouldPublish() {
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("test", 1),
                0L,
                1,
                0L,
                1,
                1
        );
        SenderResult<String> senderResult = mock(SenderResult.class);
        when(senderResult.recordMetadata()).thenReturn(metadata);
        when(senderResult.exception()).thenReturn(null);
        when(senderResult.correlationMetadata()).thenReturn("1");
        when(this.sender.send(any(Publisher.class)))
                .thenReturn(Flux.just(senderResult));

        ReactiveKafkaPublisher<Greeting> publisher = new ReactiveKafkaPublisher<>(
                "test",
                this.sender,
                this.threadPoolConfig,
                null,
                null,
                false
        );

        StepVerifier.create(publisher.publish("test", new KafkaRecord<>("key", new Greeting("from", "hello"))))
                .consumeNextWith(result -> {
                    assertThat(result.isRight(), is(true));
                    assertThat(result.getOrNull(), notNullValue());
                    assertThat(result.getOrNull().orNull(), notNullValue());
                    assertThat(result.getOrNull().orNull(), instanceOf(PublisherRecord.class));
                }).verifyComplete();

        publisher = new ReactiveKafkaPublisher<>(
                "test",
                this.sender,
                this.threadPoolConfig,
                null,
                null,
                true
        );

        StepVerifier.create(publisher.publish("test", new KafkaRecord<>("key", new Greeting("from", "hello"))))
                .consumeNextWith(result -> {
                    assertThat(result.isRight(), is(true));
                    assertThat(result.getOrNull(), notNullValue());
                    assertThat(result.getOrNull().orNull(), notNullValue());
                    assertThat(result.getOrNull().orNull(), instanceOf(PublisherRecord.class));
                }).verifyComplete();
    }

    @Test
    void shouldHandlePublishError() {
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("test", 1),
                0L,
                1,
                0L,
                1,
                1
        );
        SenderResult<String> senderResult = mock(SenderResult.class);
        when(senderResult.recordMetadata()).thenReturn(metadata);
        when(senderResult.exception()).thenReturn(new IllegalStateException("error"));
        when(senderResult.correlationMetadata()).thenReturn("1");
        when(this.sender.send(any(Publisher.class)))
                .thenReturn(Flux.just(senderResult));

        ReactiveKafkaPublisher<Greeting> publisher = new ReactiveKafkaPublisher<>(
                "test",
                this.sender,
                this.threadPoolConfig,
                null,
                null,
                false
        );

        StepVerifier.create(publisher.publish("test", new KafkaRecord<>("key", new Greeting("from", "hello"))))
                .consumeNextWith(result -> {
                    assertThat(result.isLeft(), is(true));
                    Throwable exception = result.swap().getOrNull();
                    assertThat(exception, notNullValue());
                    assertThat(exception.getMessage(), is("error"));
                    assertThat(exception, instanceOf(IllegalStateException.class));
                }).verifyComplete();

        publisher = new ReactiveKafkaPublisher<>(
                "test",
                this.sender,
                this.threadPoolConfig,
                null,
                null,
                true
        );

        StepVerifier.create(publisher.publish("test", new KafkaRecord<>("key", new Greeting("from", "hello"))))
                .consumeNextWith(result -> {
                    assertThat(result.isLeft(), is(true));
                    Throwable exception = result.swap().getOrNull();
                    assertThat(exception, notNullValue());
                    assertThat(exception.getMessage(), is("error"));
                    assertThat(exception, instanceOf(IllegalStateException.class));
                }).verifyComplete();
    }
}
