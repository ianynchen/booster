package io.github.booster.messaging.publisher.kafka;

import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.config.thread.ThreadPoolConfigGeneric;
import io.github.booster.config.thread.ThreadPoolSetting;
import io.github.booster.messaging.Greeting;
import io.github.booster.messaging.publisher.PublisherRecord;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.AsyncResult;
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

class TemplateKafkaPublisherTest {

    private KafkaTemplate<String, Greeting> template;
    
    private ThreadPoolConfigGeneric threadPoolConfig;

    @BeforeEach
    void setup() {
        this.template = mock(KafkaTemplate.class);

        this.threadPoolConfig = new ThreadPoolConfigGeneric();
        this.threadPoolConfig.setSettings(
                Map.of("test", new ThreadPoolSetting())
        );
    }

    @Test
    void shouldFailCreate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TemplateKafkaPublisher<>(
                        null,
                        this.template,
                        this.threadPoolConfig,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new TemplateKafkaPublisher<>(
                        "",
                        this.template,
                        this.threadPoolConfig,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new TemplateKafkaPublisher<>(
                        " ",
                        this.template,
                        this.threadPoolConfig,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new TemplateKafkaPublisher<Greeting>(
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
                () -> new TemplateKafkaPublisher<>(
                        "abc",
                        this.template,
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new TemplateKafkaPublisher<>(
                        "abc",
                        this.template,
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
                new TemplateKafkaPublisher<>(
                        "test",
                        this.template,
                        this.threadPoolConfig,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                ),
                notNullValue()
        );
        assertThat(
                new TemplateKafkaPublisher<>(
                        "test",
                        this.template,
                        this.threadPoolConfig,
                        null,
                        null,
                        false
                ),
                notNullValue()
        );
    }

    @Test
    void shouldPublishMessage() {
        SendResult<String, Greeting> sendResult = mock(SendResult.class);
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition("test", 1),
                0L,
                1,
                0L,
                1,
                1
        );
        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(this.template.send(any(ProducerRecord.class))).thenReturn(new AsyncResult(sendResult));

        TemplateKafkaPublisher<Greeting> publisher = new TemplateKafkaPublisher<>(
                "test",
                this.template,
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

        publisher = new TemplateKafkaPublisher<>(
                "test",
                this.template,
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
    void shouldHandlePublishFailure() {
        when(this.template.send(any(ProducerRecord.class)))
                .thenReturn(AsyncResult.forExecutionException(new IllegalStateException("error")));


        TemplateKafkaPublisher<Greeting> publisher = new TemplateKafkaPublisher<>(
                "test",
                this.template,
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

        publisher = new TemplateKafkaPublisher<>(
                "test",
                this.template,
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
