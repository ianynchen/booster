package io.github.booster.messaging.publisher.gcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.publisher.PubSubPublisherTemplate;
import com.google.cloud.spring.pubsub.support.converter.JacksonPubSubMessageConverter;
import com.google.common.util.concurrent.AbstractFuture;
import com.google.pubsub.v1.PubsubMessage;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.config.thread.ThreadPoolConfig;
import io.github.booster.config.thread.ThreadPoolSetting;
import io.github.booster.messaging.Greeting;
import io.github.booster.messaging.publisher.PublisherRecord;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.AsyncResult;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GcpPublisherTest {

    static class PullResult extends AbstractFuture<String> {

        @Override
        public String get() throws InterruptedException, ExecutionException {
            return "1";
        }

        @Override
        public String get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return "1";
        }
    }

    private PubSubPublisherTemplate publisherTemplate;

    private ThreadPoolConfig threadPoolConfig;

    @BeforeEach
    void setup() {
        this.publisherTemplate = mock(PubSubPublisherTemplate.class);
        when(this.publisherTemplate.getMessageConverter())
                .thenReturn(new JacksonPubSubMessageConverter(new ObjectMapper()));

        this.threadPoolConfig = new ThreadPoolConfig();
        this.threadPoolConfig.setSettings(
                Map.of("test", new ThreadPoolSetting())
        );
    }

    @Test
    void shouldFailCreate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GcpPublisher<Integer>(
                        null,
                        this.publisherTemplate,
                        this.threadPoolConfig,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new GcpPublisher<Integer>(
                        "",
                        this.publisherTemplate,
                        this.threadPoolConfig,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new GcpPublisher<Integer>(
                        " ",
                        this.publisherTemplate,
                        this.threadPoolConfig,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new GcpPublisher<Integer>(
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
                () -> new GcpPublisher<Integer>(
                        "abc",
                        this.publisherTemplate,
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new GcpPublisher<Integer>(
                        "abc",
                        this.publisherTemplate,
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
                new GcpPublisher<>(
                        "test",
                        this.publisherTemplate,
                        this.threadPoolConfig,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                ),
                notNullValue()
        );
        assertThat(
                new GcpPublisher<>(
                        "test",
                        this.publisherTemplate,
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
        when(this.publisherTemplate.publish(anyString(), any(PubsubMessage.class)))
                .thenReturn(new AsyncResult<>("1"));

        GcpPublisher<Greeting> publisher = new GcpPublisher<>(
                "test",
                this.publisherTemplate,
                this.threadPoolConfig,
                null,
                null,
                false
        );

        StepVerifier.create(publisher.publish("abc", new PubsubRecord<>(new Greeting("server", "hello"))))
                .consumeNextWith(result -> {
                    assertThat(result.isRight(), is(true));
                    PublisherRecord record = result.getOrNull();
                    assertThat(record, notNullValue());
                    assertThat(record.getTopic(), is("abc"));
                    assertThat(record.getRecordId(), is("1"));
                }).verifyComplete();

        publisher = new GcpPublisher<>(
                "test",
                this.publisherTemplate,
                this.threadPoolConfig,
                null,
                null,
                true
        );

        StepVerifier.create(publisher.publish("abc", new PubsubRecord<>(new Greeting("server", "hello"))))
                .consumeNextWith(result -> {
                    assertThat(result.isRight(), is(true));
                    PublisherRecord record = result.getOrNull();
                    assertThat(record, notNullValue());
                    assertThat(record.getTopic(), is("abc"));
                    assertThat(record.getRecordId(), is("1"));
                }).verifyComplete();
    }

    @Test
    void shouldHandleExceptionInPublish() {
        when(this.publisherTemplate.publish(anyString(), any(PubsubMessage.class)))
                .thenReturn(AsyncResult.forExecutionException(new IllegalStateException("error")));

        GcpPublisher<Greeting> publisher = new GcpPublisher<>(
                "test",
                this.publisherTemplate,
                this.threadPoolConfig,
                null,
                null,
                false
        );

        StepVerifier.create(publisher.publish("abc", new PubsubRecord<>(new Greeting("server", "hello"))))
                .consumeNextWith(result -> {
                    assertThat(result.isLeft(), is(true));
                    Throwable exception = result.swap().getOrNull();
                    assertThat(exception, notNullValue());
                    assertThat(exception.getMessage(), is("error"));
                    assertThat(exception, instanceOf(IllegalStateException.class));
                }).verifyComplete();

        publisher = new GcpPublisher<>(
                "test",
                this.publisherTemplate,
                this.threadPoolConfig,
                null,
                null,
                true
        );

        StepVerifier.create(publisher.publish("abc", new PubsubRecord<>(new Greeting("server", "hello"))))
                .consumeNextWith(result -> {
                    assertThat(result.isLeft(), is(true));
                    Throwable exception = result.swap().getOrNull();
                    assertThat(exception, notNullValue());
                    assertThat(exception.getMessage(), is("error"));
                    assertThat(exception, instanceOf(IllegalStateException.class));
                }).verifyComplete();
    }
}
