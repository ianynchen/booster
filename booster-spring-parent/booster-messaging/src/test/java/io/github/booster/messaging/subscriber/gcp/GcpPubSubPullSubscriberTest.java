package io.github.booster.messaging.subscriber.gcp;

import com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberTemplate;
import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.config.thread.ThreadPoolConfigGeneric;
import io.github.booster.config.thread.ThreadPoolSetting;
import io.github.booster.messaging.config.GcpPubSubSubscriberConfig;
import io.github.booster.messaging.config.GcpPubSubSubscriberSetting;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GcpPubSubPullSubscriberTest {

    private AtomicInteger counter;

    private PubSubSubscriberTemplate sparseTemplate;

    private PubSubSubscriberTemplate template;

    private static final OpenTelemetryConfig openTelemetryConfig = new OpenTelemetryConfig(null, "test");

    @BeforeEach
    void setup() {
        counter = new AtomicInteger(0);

        this.sparseTemplate = mock(PubSubSubscriberTemplate.class);
        lenient().when(sparseTemplate.pull(anyString(), anyInt(), anyBoolean()))
                .thenAnswer(invocation -> {
                    int count = counter.getAndIncrement();
                    if ((count & (count - 1)) == 0) {
                        return List.of(mock(AcknowledgeablePubsubMessage.class));
                    } else {
                        return List.of();
                    }
                });

        this.template = mock(PubSubSubscriberTemplate.class);
        lenient().when(template.pull(anyString(), anyInt(), anyBoolean()))
                .thenReturn(List.of(mock(AcknowledgeablePubsubMessage.class)));
    }

    private ThreadPoolConfigGeneric createThreadConfig() {
        ThreadPoolConfigGeneric config = new ThreadPoolConfigGeneric();
        config.setSettings(Map.of("test", new ThreadPoolSetting()));
        return config;
    }

    private GcpPubSubSubscriberConfig createGcpPubSubSubscriberConfig() {
        GcpPubSubSubscriberConfig config = new GcpPubSubSubscriberConfig();
        GcpPubSubSubscriberSetting setting = new GcpPubSubSubscriberSetting();
        setting.setSubscription("test");
        setting.setMaxRecords(10);
        config.setSettings(Map.of("test", setting));
        return config;
    }

    @Test
    void shouldFailToCreate() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new GcpPubSubPullSubscriber(
                        null,
                        this.template,
                        new ThreadPoolConfigGeneric(),
                        new GcpPubSubSubscriberConfig(),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new GcpPubSubPullSubscriber(
                        " ",
                        this.template,
                        new ThreadPoolConfigGeneric(),
                        new GcpPubSubSubscriberConfig(),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new GcpPubSubPullSubscriber(
                        "test",
                        null,
                        new ThreadPoolConfigGeneric(),
                        new GcpPubSubSubscriberConfig(),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new GcpPubSubPullSubscriber(
                        "test",
                        this.template,
                        null,
                        new GcpPubSubSubscriberConfig(),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new GcpPubSubPullSubscriber(
                        "test",
                        this.template,
                        new ThreadPoolConfigGeneric(),
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new GcpPubSubPullSubscriber(
                        "abc",
                        this.template,
                        createThreadConfig(),
                        createGcpPubSubSubscriberConfig(),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new GcpPubSubPullSubscriber(
                        "test",
                        this.template,
                        createThreadConfig(),
                        new GcpPubSubSubscriberConfig(),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                )
        );
    }

    @Test
    void shouldCreate() {
        assertThat(
                new GcpPubSubPullSubscriber(
                        "test",
                        this.template,
                        createThreadConfig(),
                        createGcpPubSubSubscriberConfig(),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        null,
                        false
                ),
                notNullValue()
        );

        assertThat(
                new GcpPubSubPullSubscriber(
                        "test",
                        this.template,
                        createThreadConfig(),
                        createGcpPubSubSubscriberConfig(),
                        null,
                        null,
                        false
                ),
                notNullValue()
        );
    }

    @Test
    void shouldPullRecordsWithOTEL() {
        GcpPubSubPullSubscriber subscriber = new GcpPubSubPullSubscriber(
                "test",
                this.template,
                createThreadConfig(),
                createGcpPubSubSubscriberConfig(),
                null,
                openTelemetryConfig,
                false
        );
        StepVerifier.create(subscriber.flux().take(5))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .verifyComplete();

        subscriber = new GcpPubSubPullSubscriber(
                "test",
                this.template,
                createThreadConfig(),
                createGcpPubSubSubscriberConfig(),
                null,
                openTelemetryConfig,
                true
        );
        StepVerifier.create(subscriber.flux().take(5))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .verifyComplete();
    }

    @Test
    void shouldPullRecordsWithoutOTEL() {
        GcpPubSubPullSubscriber subscriber = new GcpPubSubPullSubscriber(
                "test",
                this.template,
                createThreadConfig(),
                createGcpPubSubSubscriberConfig(),
                null,
                null,
                false
        );
        StepVerifier.create(subscriber.flux().take(5))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .verifyComplete();

        subscriber = new GcpPubSubPullSubscriber(
                "test",
                this.template,
                createThreadConfig(),
                createGcpPubSubSubscriberConfig(),
                null,
                null,
                true
        );
        StepVerifier.create(subscriber.flux().take(5))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .consumeNextWith(list -> assertThat(list, hasSize(1)))
                .verifyComplete();
    }

    @Test
    void shouldPullFlatRecordsWithOTEL() {
        GcpPubSubPullSubscriber subscriber = new GcpPubSubPullSubscriber(
                "test",
                this.template,
                createThreadConfig(),
                createGcpPubSubSubscriberConfig(),
                null,
                openTelemetryConfig,
                false
        );

        StepVerifier.create(subscriber.flatFlux().take(5))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .verifyComplete();
        subscriber.stop();

        subscriber = new GcpPubSubPullSubscriber(
                "test",
                this.template,
                createThreadConfig(),
                createGcpPubSubSubscriberConfig(),
                null,
                openTelemetryConfig,
                true
        );

        StepVerifier.create(subscriber.flatFlux().take(5))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .verifyComplete();
        subscriber.stop();
    }

    @Test
    void shouldPullFlatRecordsWithoutOTEL() {
        GcpPubSubPullSubscriber subscriber = new GcpPubSubPullSubscriber(
                "test",
                this.template,
                createThreadConfig(),
                createGcpPubSubSubscriberConfig(),
                null,
                null,
                false
        );

        StepVerifier.create(subscriber.flatFlux().take(5))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .verifyComplete();
        subscriber.stop();

        subscriber = new GcpPubSubPullSubscriber(
                "test",
                this.template,
                createThreadConfig(),
                createGcpPubSubSubscriberConfig(),
                null,
                null,
                true
        );

        StepVerifier.create(subscriber.flatFlux().take(5))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .consumeNextWith(item -> assertThat(item, instanceOf(AcknowledgeablePubsubMessage.class)))
                .verifyComplete();
        subscriber.stop();
    }

    @Test
    void shouldHandlePullError() {
        PubSubSubscriberTemplate template = mock(PubSubSubscriberTemplate.class);
        AtomicInteger count = new AtomicInteger(0);
        when(template.pull(anyString(), anyInt(), anyBoolean()))
                .thenAnswer(invocation -> {
                    if (count.getAndIncrement() == 2) {
                        return List.of(mock(AcknowledgeablePubsubMessage.class));
                    } else {
                        throw new IllegalStateException("error");
                    }
                });

        GcpPubSubPullSubscriber subscriber = new GcpPubSubPullSubscriber(
                "test",
                template,
                createThreadConfig(),
                createGcpPubSubSubscriberConfig(),
                null,
                openTelemetryConfig,
                false
        );

        StepVerifier.create(subscriber.flux().take(1))
                .consumeNextWith(record -> assertThat(record, hasSize(1)))
                .verifyComplete();
        subscriber.stop();

        count.set(0);
        subscriber = new GcpPubSubPullSubscriber(
                "test",
                template,
                createThreadConfig(),
                createGcpPubSubSubscriberConfig(),
                null,
                openTelemetryConfig,
                false
        );

        StepVerifier.create(subscriber.flux().take(1))
                .consumeNextWith(record -> assertThat(record, hasSize(1)))
                .verifyComplete();
        subscriber.stop();
    }
}
