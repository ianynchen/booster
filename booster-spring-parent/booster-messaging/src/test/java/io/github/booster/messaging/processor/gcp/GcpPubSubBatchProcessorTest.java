package io.github.booster.messaging.processor.gcp;

import arrow.core.Option;
import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.messaging.subscriber.gcp.MockGcpBatchSubscriberFlow;
import io.github.booster.task.Task;
import io.github.booster.task.TaskExecutionContext;
import io.github.booster.task.impl.AsyncTask;
import io.github.booster.task.impl.RequestHandlers;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import lombok.val;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GcpPubSubBatchProcessorTest {

    Function1<Throwable, Option<List<AcknowledgeablePubsubMessage>>> requestExceptionHandler =
            (t) -> {
                throw new IllegalArgumentException(t);
            };

    Function0<Option<List<AcknowledgeablePubsubMessage>>> emptyRequestHandler =
            () -> Option.fromNullable(null);

    Function1<List<AcknowledgeablePubsubMessage>, Mono<Option<List<AcknowledgeablePubsubMessage>>>> process =
            (list) -> Mono.just(Option.fromNullable(list));

    Function1<List<AcknowledgeablePubsubMessage>, Mono<Option<List<AcknowledgeablePubsubMessage>>>> exceptionProcess =
            (data) -> Mono.error(new IllegalStateException("error"));

    Function1<List<AcknowledgeablePubsubMessage>, Mono<Option<List<AcknowledgeablePubsubMessage>>>> partialFailureProcess =
            (messages) -> Mono.just(
                    Option.fromNullable(
                            messages.stream()
                                    .filter(msg -> {
                                        String value = msg.getPubsubMessage().getData().toString(StandardCharsets.UTF_8);
                                        return !Objects.equals(value, "3");
                                    })
                                    .collect(Collectors.toList())
                    )
            );

    private final Task<List<AcknowledgeablePubsubMessage>, List<AcknowledgeablePubsubMessage>> task =
            new AsyncTask<>(
                    "test",
                    new RequestHandlers<>(
                            Option.fromNullable(emptyRequestHandler),
                            Option.fromNullable(requestExceptionHandler)
                    ),
                    new TaskExecutionContext(
                            Option.fromNullable(null),
                            Option.fromNullable(null),
                            Option.fromNullable(null),
                            new MetricsRegistry(new SimpleMeterRegistry())
                    ),
                    this.process
            );

    private final Task<List<AcknowledgeablePubsubMessage>, List<AcknowledgeablePubsubMessage>> partialFailureTask =
            new AsyncTask<>(
                    "test",
                    new RequestHandlers<>(
                            Option.fromNullable(emptyRequestHandler),
                            Option.fromNullable(requestExceptionHandler)
                    ),
                    new TaskExecutionContext(
                            Option.fromNullable(null),
                            Option.fromNullable(null),
                            Option.fromNullable(null),
                            new MetricsRegistry(new SimpleMeterRegistry())
                    ),
                    this.partialFailureProcess
            );

    private final Task<List<AcknowledgeablePubsubMessage>, List<AcknowledgeablePubsubMessage>> errorTask =
            new AsyncTask<>(
                    "test",
                    new RequestHandlers<>(
                            Option.fromNullable(emptyRequestHandler),
                            Option.fromNullable(requestExceptionHandler)
                    ),
                    new TaskExecutionContext(
                            Option.fromNullable(null),
                            Option.fromNullable(null),
                            Option.fromNullable(null),
                            new MetricsRegistry(new SimpleMeterRegistry())
                    ),
                    this.exceptionProcess
            );

    @Test
    void shouldFailCreate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GcpPubSubBatchProcessor(
                        null,
                        task,
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new GcpPubSubBatchProcessor(
                        new MockGcpBatchSubscriberFlow("test", true),
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
                new GcpPubSubBatchProcessor(
                        new MockGcpBatchSubscriberFlow("test", true),
                        task,
                        null,
                        null,
                        false
                ),
                notNullValue()
        );
        assertThat(
                new GcpPubSubBatchProcessor(
                        new MockGcpBatchSubscriberFlow("test", false),
                        task,
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        true
                ),
                notNullValue()
        );
    }

    @Test
    void shouldExecuteAndAcknowledge() {
        GcpPubSubBatchProcessor processor = new GcpPubSubBatchProcessor(
                new MockGcpBatchSubscriberFlow("test", true),
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
                        assertThat(record.getData(), hasSize(i + 1));
                        assertThat(record.getAcknowledged(), is(i + 1));

                        for (int j = 0; j < i + 1; j++) {
                            String value = record.getData().get(j).getPubsubMessage().getData().toString(StandardCharsets.UTF_8);
                            assertThat(value, equalTo(Integer.toString(j)));
                        }
                    }
                }).verifyComplete();

        processor = new GcpPubSubBatchProcessor(
                new MockGcpBatchSubscriberFlow("test", true),
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
                        assertThat(record.getData(), hasSize(i + 1));
                        assertThat(record.getAcknowledged(), is(i + 1));

                        for (int j = 0; j < i + 1; j++) {
                            String value = record.getData().get(j).getPubsubMessage().getData().toString(StandardCharsets.UTF_8);
                            assertThat(value, equalTo(Integer.toString(j)));
                        }
                    }
                }).verifyComplete();
    }

    @Test
    void shouldExecuteAndFailAcknowledge() {
        GcpPubSubBatchProcessor processor = new GcpPubSubBatchProcessor(
                new MockGcpBatchSubscriberFlow("test", false),
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
                        assertThat(record.getData(), hasSize(i + 1));
                        assertThat(record.getAcknowledged(), is(0));

                        for (int j = 0; j < i + 1; j++) {
                            String value = record.getData().get(j).getPubsubMessage().getData().toString(StandardCharsets.UTF_8);
                            assertThat(value, equalTo(Integer.toString(j)));
                        }
                    }
                }).verifyComplete();

        processor = new GcpPubSubBatchProcessor(
                new MockGcpBatchSubscriberFlow("test", false),
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
                        assertThat(record.getData(), hasSize(i + 1));
                        assertThat(record.getAcknowledged(), is(0));

                        for (int j = 0; j < i + 1; j++) {
                            String value = record.getData().get(j).getPubsubMessage().getData().toString(StandardCharsets.UTF_8);
                            assertThat(value, equalTo(Integer.toString(j)));
                        }
                    }
                }).verifyComplete();
    }

    @Test
    void shouldRecordFailure() {
        GcpPubSubBatchProcessor processor = new GcpPubSubBatchProcessor(
                new MockGcpBatchSubscriberFlow("test", false),
                errorTask,
                null,
                new MetricsRegistry(new SimpleMeterRegistry()),
                false
        );

        StepVerifier.create(processor.process().take(5).collectList())
                .consumeNextWith(list -> {
                    assertThat(list, hasSize(5));
                    for (int i = 0; i < list.size(); i++) {
                        assertThat(list.get(i).isRight(), is(false));
                        Throwable t = list.get(i).swap().getOrNull();
                        assertThat(t, notNullValue());
                        assertThat(t, instanceOf(IllegalStateException.class));
                        assertThat(t.getMessage(), is("error"));
                    }
                }).verifyComplete();

        processor = new GcpPubSubBatchProcessor(
                new MockGcpBatchSubscriberFlow("test", false),
                errorTask,
                null,
                new MetricsRegistry(new SimpleMeterRegistry()),
                true
        );

        StepVerifier.create(processor.process().take(5).collectList())
                .consumeNextWith(list -> {
                    assertThat(list, hasSize(5));
                    for (int i = 0; i < list.size(); i++) {
                        assertThat(list.get(i).isRight(), is(false));
                        Throwable t = list.get(i).swap().getOrNull();
                        assertThat(t, notNullValue());
                        assertThat(t, instanceOf(IllegalStateException.class));
                        assertThat(t.getMessage(), is("error"));
                    }
                }).verifyComplete();
    }

    @Test
    void shouldHandlePartialFailure() {
        GcpPubSubBatchProcessor processor = new GcpPubSubBatchProcessor(
                new MockGcpBatchSubscriberFlow("test", true),
                partialFailureTask,
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

                        if (i < 3) {
                            assertThat(record.getData(), hasSize(i + 1));
                            assertThat(record.getAcknowledged(), is(i + 1));
                        } else {
                            assertThat(record.getData(), hasSize(i));
                            assertThat(record.getAcknowledged(), is(i));
                        }
                    }
                }).verifyComplete();

        processor = new GcpPubSubBatchProcessor(
                new MockGcpBatchSubscriberFlow("test", true),
                partialFailureTask,
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

                        if (i < 3) {
                            assertThat(record.getData(), hasSize(i + 1));
                            assertThat(record.getAcknowledged(), is(i + 1));
                        } else {
                            assertThat(record.getData(), hasSize(i));
                            assertThat(record.getAcknowledged(), is(i));
                        }
                    }
                }).verifyComplete();
    }
}
