package io.github.booster.messaging.processor.kafka;

import arrow.core.Option;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.messaging.processor.ProcessResult;
import io.github.booster.messaging.subscriber.kafka.MockKafkaSubscriberFlow;
import io.github.booster.messaging.subscriber.kafka.SubscriberRecord;
import io.github.booster.task.Task;
import io.github.booster.task.impl.AsyncTask;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import kotlin.jvm.functions.Function1;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KafkaProcessorTest {

    Function1<SubscriberRecord<Integer>, Mono<SubscriberRecord<Integer>>> process =
            Mono::just;

    Function1<SubscriberRecord<Integer>, Mono<SubscriberRecord<Integer>>> errorProcess =
            (data) -> {
                throw new IllegalArgumentException("");
            };

    Function1<Throwable, SubscriberRecord<Integer>> exceptionProcess =
            (either) -> {
                throw new IllegalStateException("error");
            };

    private final Task<SubscriberRecord<Integer>, SubscriberRecord<Integer>> task =
            new AsyncTask<>(
                    "test",
                    Option.fromNullable(null),
                    Option.fromNullable(null),
                    Option.fromNullable(null),
                    new MetricsRegistry(new SimpleMeterRegistry()),
                    process,
                    exceptionProcess
            );

    @Test
    void shouldFailCreate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaProcessor<>(
                        null,
                        task,
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new KafkaProcessor(
                        new MockKafkaSubscriberFlow("test"),
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
                new KafkaProcessor<>(
                        new MockKafkaSubscriberFlow("test"),
                        task,
                        null,
                        null,
                        false
                ),
                notNullValue()
        );
        assertThat(
                new KafkaProcessor<>(
                        new MockKafkaSubscriberFlow("test"),
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
        KafkaProcessor<Integer> processor = new KafkaProcessor<>(
                new MockKafkaSubscriberFlow("test"),
                task,
                null,
                new MetricsRegistry(new SimpleMeterRegistry()),
                false
        );
        StepVerifier.create(processor.process().take(5).collectList())
                .consumeNextWith(list -> {
                    for (int i = 0; i < 5; i++) {
                        assertThat(list.get(i).isRight(), is(true));
                        ProcessResult<SubscriberRecord<Integer>> record = list.get(i).getOrNull();
                        assertThat(record, notNullValue());
                        assertThat(record.getData().getData(), is(i));
                        assertThat(record.isAcknowledged(), is(true));
                    }
                }).verifyComplete();

        processor = new KafkaProcessor<>(
                new MockKafkaSubscriberFlow("test"),
                task,
                null,
                new MetricsRegistry(new SimpleMeterRegistry()),
                true
        );
        StepVerifier.create(processor.process().take(5).collectList())
                .consumeNextWith(list -> {
                    for (int i = 0; i < 5; i++) {
                        assertThat(list.get(i).isRight(), is(true));
                        ProcessResult<SubscriberRecord<Integer>> record = list.get(i).getOrNull();
                        assertThat(record, notNullValue());
                        assertThat(record.getData().getData(), is(i));
                        assertThat(record.isAcknowledged(), is(true));
                    }
                }).verifyComplete();
    }

    @Test
    void shouldExecuteAndNotAcknowledge() {
        KafkaProcessor<Integer> processor = new KafkaProcessor<>(
                new MockKafkaSubscriberFlow("test"),
                new AsyncTask<>(
                        "test",
                        Option.fromNullable(null),
                        Option.fromNullable(null),
                        Option.fromNullable(null),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        (data) -> Mono.error(new IllegalArgumentException("error")),
                        this.exceptionProcess
                ),
                null,
                new MetricsRegistry(new SimpleMeterRegistry()),
                false
        );
        StepVerifier.create(processor.process().take(5).collectList())
                .consumeNextWith(list -> {
                    for (int i = 0; i < 5; i++) {
                        assertThat(list.get(i).isLeft(), is(true));
                    }
                }).verifyComplete();

        processor = new KafkaProcessor<>(
                new MockKafkaSubscriberFlow("test"),
                new AsyncTask<>(
                        "test",
                        Option.fromNullable(null),
                        Option.fromNullable(null),
                        Option.fromNullable(null),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        this.errorProcess,
                        this.exceptionProcess
                ),
                null,
                new MetricsRegistry(new SimpleMeterRegistry()),
                true
        );
        StepVerifier.create(processor.process().take(5).collectList())
                .consumeNextWith(list -> {
                    for (int i = 0; i < 5; i++) {
                        assertThat(list.get(i).isLeft(), is(true));
                    }
                }).verifyComplete();
    }

    @Test
    void shouldExecuteAndFailAcknowledge() {
        KafkaProcessor<Integer> processor = new KafkaProcessor<>(
                new MockKafkaSubscriberFlow("test", false),
                task,
                null,
                new MetricsRegistry(new SimpleMeterRegistry()),
                false
        );
        StepVerifier.create(processor.process().take(5).collectList())
                .consumeNextWith(list -> {
                    for (int i = 0; i < 5; i++) {
                        assertThat(list.get(i).isRight(), is(true));
                        ProcessResult<SubscriberRecord<Integer>> record = list.get(i).getOrNull();
                        assertThat(record, notNullValue());
                        assertThat(record.getData().getData(), is(i));
                        assertThat(record.isAcknowledged(), is(false));
                    }
                }).verifyComplete();

        processor = new KafkaProcessor<>(
                new MockKafkaSubscriberFlow("test", false),
                task,
                null,
                new MetricsRegistry(new SimpleMeterRegistry()),
                true
        );
        StepVerifier.create(processor.process().take(5).collectList())
                .consumeNextWith(list -> {
                    for (int i = 0; i < 5; i++) {
                        assertThat(list.get(i).isRight(), is(true));
                        ProcessResult<SubscriberRecord<Integer>> record = list.get(i).getOrNull();
                        assertThat(record, notNullValue());
                        assertThat(record.getData().getData(), is(i));
                        assertThat(record.isAcknowledged(), is(false));
                    }
                }).verifyComplete();
    }
}
