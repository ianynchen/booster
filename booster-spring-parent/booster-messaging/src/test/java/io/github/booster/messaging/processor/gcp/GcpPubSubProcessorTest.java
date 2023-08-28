package io.github.booster.messaging.processor.gcp;

import arrow.core.Option;
import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.messaging.processor.ProcessResult;
import io.github.booster.messaging.publisher.PublisherRecord;
import io.github.booster.messaging.subscriber.gcp.MockGcpSubscriberFlow;
import io.github.booster.task.ExecutionType;
import io.github.booster.task.Task;
import io.github.booster.task.TaskExecutionContext;
import io.github.booster.task.impl.AsyncTask;
import io.github.booster.task.impl.RequestHandlers;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import lombok.val;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GcpPubSubProcessorTest {

    Function0<Option<AcknowledgeablePubsubMessage>> emptyRequestHandler =
            () -> Option.fromNullable(null);

    Function1<AcknowledgeablePubsubMessage, Mono<Option<AcknowledgeablePubsubMessage>>> process =
            (record) -> Mono.just(Option.fromNullable(record));

    Function1<Throwable, Option<AcknowledgeablePubsubMessage>> exceptionProcess =
            (throwable) -> {
                throw new IllegalStateException("error");
            };

    private final Task<AcknowledgeablePubsubMessage, AcknowledgeablePubsubMessage> task =
            new AsyncTask<>(
                    "test",
                    new RequestHandlers<>(
                            Option.fromNullable(emptyRequestHandler),
                            Option.fromNullable(exceptionProcess)
                    ),
                    new TaskExecutionContext(
                            Option.fromNullable(null),
                            Option.fromNullable(null),
                            Option.fromNullable(null),
                            new MetricsRegistry(new SimpleMeterRegistry()),
                            ExecutionType.PUBLISH_ON
                    ),
                    process
            );

    @Test
    void shouldFailCreate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GcpPubSubProcessor(
                        null,
                        task,
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new GcpPubSubProcessor(
                        new MockGcpSubscriberFlow("test", true),
                        null,
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                )
        );
    }

    @Test
    void shouldCreate() {
        assertThat(
                new GcpPubSubProcessor(
                        new MockGcpSubscriberFlow("test", true),
                        task,
                        null,
                        null,
                        false
                ),
                notNullValue()
        );
        assertThat(
                new GcpPubSubProcessor(
                        new MockGcpSubscriberFlow("test", false),
                        task,
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                ),
                notNullValue()
        );
    }

    @Test
    void shouldExecuteAndAcknowledge() {
        GcpPubSubProcessor processor = new GcpPubSubProcessor(
                new MockGcpSubscriberFlow("test", true),
                task,
                null,
                new MetricsRegistry(new SimpleMeterRegistry()),
                false
        );

        StepVerifier.create(processor.process().take(5).collectList())
                .consumeNextWith(list -> {
                    assertThat(list, hasSize(5));
                    for (int i = 0; i < list.size(); i++) {
                        assertThat(list.get(i).isRight(), is(true));

                        val recordOption = list.get(i).getOrNull();
                        assertThat(recordOption, notNullValue());

                        ProcessResult<AcknowledgeablePubsubMessage> record = recordOption.orNull();
                        assertThat(record, notNullValue());
                        assertThat(record.isAcknowledged(), is(true));
                        assertThat(record.getData().getPubsubMessage().getData().toString(StandardCharsets.UTF_8), equalTo(Integer.toString(i)));
                    }
                }).verifyComplete();

        processor = new GcpPubSubProcessor(
                new MockGcpSubscriberFlow("test", true),
                task,
                null,
                new MetricsRegistry(new SimpleMeterRegistry()),
                true
        );

        StepVerifier.create(processor.process().take(5).collectList())
                .consumeNextWith(list -> {
                    assertThat(list, hasSize(5));
                    for (int i = 0; i < list.size(); i++) {
                        assertThat(list.get(i).isRight(), is(true));
                        val recordOption = list.get(i).getOrNull();
                        assertThat(recordOption, notNullValue());

                        val record = recordOption.orNull();
                        assertThat(record, notNullValue());
                        assertThat(record.isAcknowledged(), is(true));
                        assertThat(record.getData().getPubsubMessage().getData().toString(StandardCharsets.UTF_8), equalTo(Integer.toString(i)));
                    }
                }).verifyComplete();
    }

    @Test
    void shouldExecuteAndFailAcknowledge() {
        GcpPubSubProcessor processor = new GcpPubSubProcessor(
                new MockGcpSubscriberFlow("test", false),
                task,
                null,
                new MetricsRegistry(new SimpleMeterRegistry()),
                false
        );

        StepVerifier.create(processor.process().take(5).collectList())
                .consumeNextWith(list -> {
                    assertThat(list, hasSize(5));
                    for (int i = 0; i < list.size(); i++) {
                        assertThat(list.get(i).isRight(), is(true));
                        val recordOption = list.get(i).getOrNull();
                        assertThat(recordOption, notNullValue());

                        val record = recordOption.orNull();
                        assertThat(record, notNullValue());
                        assertThat(record.isAcknowledged(), is(false));
                        assertThat(record.getData().getPubsubMessage().getData().toString(StandardCharsets.UTF_8), equalTo(Integer.toString(i)));
                    }
                }).verifyComplete();

        processor = new GcpPubSubProcessor(
                new MockGcpSubscriberFlow("test", false),
                task,
                null,
                new MetricsRegistry(new SimpleMeterRegistry()),
                true
        );

        StepVerifier.create(processor.process().take(5).collectList())
                .consumeNextWith(list -> {
                    assertThat(list, hasSize(5));
                    for (int i = 0; i < list.size(); i++) {
                        assertThat(list.get(i).isRight(), is(true));
                        val recordOption = list.get(i).getOrNull();
                        assertThat(recordOption, notNullValue());

                        val record = recordOption.orNull();
                        assertThat(record, notNullValue());
                        assertThat(record.isAcknowledged(), is(false));
                        assertThat(record.getData().getPubsubMessage().getData().toString(StandardCharsets.UTF_8), equalTo(Integer.toString(i)));
                    }
                }).verifyComplete();
    }
}
