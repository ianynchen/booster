package io.github.booster.messaging.processor.aws;

import arrow.core.Option;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.messaging.config.AwsSqsConfig;
import io.github.booster.messaging.config.AwsSqsSetting;
import io.github.booster.messaging.subscriber.aws.MockAwsBatchSubscriberFlow;
import io.github.booster.task.ExecutionType;
import io.github.booster.task.Task;
import io.github.booster.task.TaskExecutionContext;
import io.github.booster.task.impl.AsyncTask;
import io.github.booster.task.impl.RequestHandlers;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.BatchEntryIdsNotDistinctException;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResultEntry;
import software.amazon.awssdk.services.sqs.model.EmptyBatchRequestException;
import software.amazon.awssdk.services.sqs.model.InvalidBatchEntryIdException;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SqsException;
import software.amazon.awssdk.services.sqs.model.TooManyEntriesInBatchRequestException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AwsSqsBatchProcessorTest {

    static class MockSqsClient implements SqsClient {

        private List<DeleteMessageBatchRequestEntry> entries;

        private final List<String> failEntries;

        private final boolean alwaysReturnNull;

        public MockSqsClient(List<String> failEntries) {
            this.failEntries = failEntries;
            this.alwaysReturnNull = false;
        }

        public MockSqsClient() {
            this.alwaysReturnNull = true;
            this.failEntries = null;
        }

        @Override
        public DeleteMessageBatchResponse deleteMessageBatch(DeleteMessageBatchRequest deleteMessageBatchRequest) throws TooManyEntriesInBatchRequestException, EmptyBatchRequestException, BatchEntryIdsNotDistinctException, InvalidBatchEntryIdException, AwsServiceException, SdkClientException, SqsException {
            DeleteMessageBatchResponse response = mock(DeleteMessageBatchResponse.class);
            List<DeleteMessageBatchResultEntry> responseEntries = new ArrayList<>();
            deleteMessageBatchRequest.entries().forEach(entry -> {
                if (!this.failEntries.contains(entry.id())) {
                    DeleteMessageBatchResultEntry resultEntry = mock(DeleteMessageBatchResultEntry.class);
                    when(resultEntry.id()).thenReturn(entry.id());
                    responseEntries.add(resultEntry);
                }
            });
            if (!this.alwaysReturnNull) {
                when(response.successful()).thenReturn(responseEntries);
                return response;
            } else {
                return null;
            }
        }

        @Override
        public String serviceName() {
            return "test";
        }

        @Override
        public void close() {}
    }

    private final AwsSqsConfig awsSqsConfig = new AwsSqsConfig();

    Function1<List<Message>, Mono<Option<List<Message>>>> process =
            (records) -> Mono.just(Option.fromNullable(records));

    Function1<List<Message>, Mono<Option<List<Message>>>> exceptionProcess =
            (o) -> Mono.error(new IllegalStateException("error"));

    Function1<List<Message>, Mono<Option<List<Message>>>> partialFailureProcess =
            (messages) -> Mono.just(
                    Option.fromNullable(
                            messages.stream()
                                    .filter(msg -> {
                                        String value = msg.body();
                                        return !Objects.equals(value, "3");
                                    })
                                    .collect(Collectors.toList())
                    )
            );

    Function0<Option<List<Message>>> emptyRequestHandler =
            () -> Option.fromNullable(null);

    Function1<Throwable, Option<List<Message>>> requestExceptionHandler =
            (t) -> { throw new IllegalArgumentException(t); };

    private final Task<List<Message>, List<Message>> task =
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
                            new MetricsRegistry(new SimpleMeterRegistry()),
                            ExecutionType.PUBLISH_ON
                    ),
                    this.process
            );

    private final Task<List<Message>, List<Message>> partialFailureTask =
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
                            new MetricsRegistry(new SimpleMeterRegistry()),
                            ExecutionType.PUBLISH_ON
                    ),
                    this.partialFailureProcess
            );

    private final Task<List<Message>, List<Message>> errorTask =
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
                            new MetricsRegistry(new SimpleMeterRegistry()),
                            ExecutionType.PUBLISH_ON
                    ),
                    this.exceptionProcess
            );

    @BeforeEach
    void setup() {
        AwsSqsSetting awsSqsSetting = new AwsSqsSetting();
        awsSqsSetting.setRegion(Region.AF_SOUTH_1);
        awsSqsSetting.setQueueUrl("http://test");
        this.awsSqsConfig.setSettings(Map.of("test", awsSqsSetting));
    }

    @Test
    void shouldFailCreate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsBatchProcessor(
                        null,
                        this.awsSqsConfig,
                        this.task,
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsBatchProcessor(
                        new MockAwsBatchSubscriberFlow("test"),
                        null,
                        this.task,
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsBatchProcessor(
                        new MockAwsBatchSubscriberFlow("test"),
                        new AwsSqsConfig(),
                        this.task,
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsBatchProcessor(
                        new MockAwsBatchSubscriberFlow("test"),
                        this.awsSqsConfig,
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
                new AwsSqsBatchProcessor(
                        new MockAwsBatchSubscriberFlow("test"),
                        this.awsSqsConfig,
                        task,
                        null,
                        null,
                        true
                ),
                notNullValue()
        );
        assertThat(
                new AwsSqsBatchProcessor(
                        new MockAwsBatchSubscriberFlow("test"),
                        this.awsSqsConfig,
                        task,
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                ),
                notNullValue()
        );
    }

    private AwsSqsSetting createMockSetting(List<String> failEntries) {
        AwsSqsSetting mockSetting = mock(AwsSqsSetting.class);
        when(mockSetting.getQueueUrl()).thenReturn("http://test");
        when(mockSetting.getReceiverSetting()).thenReturn(new AwsSqsSetting.ReceiverSetting());

        SqsClient mockClient = new MockSqsClient(failEntries);
        when(mockSetting.createClient()).thenReturn(mockClient);
        return mockSetting;
    }

    private AwsSqsSetting createMockSettingWithNullResponse() {
        AwsSqsSetting mockSetting = mock(AwsSqsSetting.class);
        when(mockSetting.getQueueUrl()).thenReturn("http://test");
        when(mockSetting.getReceiverSetting()).thenReturn(new AwsSqsSetting.ReceiverSetting());

        SqsClient mockClient = new MockSqsClient();
        when(mockSetting.createClient()).thenReturn(mockClient);
        return mockSetting;
    }

    @Test
    void shouldExecuteAndAcknowledge() {
        AwsSqsConfig localConfig = new AwsSqsConfig();
        localConfig.setSettings(Map.of("test", this.createMockSetting(List.of())));

        AwsSqsBatchProcessor processor = new AwsSqsBatchProcessor(
                new MockAwsBatchSubscriberFlow("test"),
                localConfig,
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
                            String value = record.getData().get(j).body();
                            assertThat(value, equalTo(Integer.toString(j)));
                        }
                    }
                }).verifyComplete();

        processor = new AwsSqsBatchProcessor(
                new MockAwsBatchSubscriberFlow("test"),
                localConfig,
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
                            String value = record.getData().get(j).body();
                            assertThat(value, equalTo(Integer.toString(j)));
                        }
                    }
                }).verifyComplete();
    }

    @Test
    void shouldExecuteAndFailAcknowledge() {
        AwsSqsConfig localConfig = new AwsSqsConfig();
        localConfig.setSettings(Map.of("test", this.createMockSetting(List.of("0", "1", "2", "3", "4", "5"))));

        AwsSqsBatchProcessor processor = new AwsSqsBatchProcessor(
                new MockAwsBatchSubscriberFlow("test"),
                localConfig,
                this.task,
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
                            String value = record.getData().get(j).body();
                            assertThat(value, equalTo(Integer.toString(j)));
                        }
                    }
                }).verifyComplete();

        processor = new AwsSqsBatchProcessor(
                new MockAwsBatchSubscriberFlow("test"),
                localConfig,
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
                            String value = record.getData().get(j).body();
                            assertThat(value, equalTo(Integer.toString(j)));
                        }
                    }
                }).verifyComplete();
    }

    @Test
    void shouldExecuteAndFailAcknowledgeWithNullResponse() {
        AwsSqsConfig localConfig = new AwsSqsConfig();
        localConfig.setSettings(Map.of("test", this.createMockSettingWithNullResponse()));

        AwsSqsBatchProcessor processor = new AwsSqsBatchProcessor(
                new MockAwsBatchSubscriberFlow("test"),
                localConfig,
                this.task,
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
                            String value = record.getData().get(j).body();
                            assertThat(value, equalTo(Integer.toString(j)));
                        }
                    }
                }).verifyComplete();

        processor = new AwsSqsBatchProcessor(
                new MockAwsBatchSubscriberFlow("test"),
                localConfig,
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
                            String value = record.getData().get(j).body();
                            assertThat(value, equalTo(Integer.toString(j)));
                        }
                    }
                }).verifyComplete();
    }

    @Test
    void shouldExecuteAndFailAcknowledgeWithException() {
        AwsSqsConfig localConfig = new AwsSqsConfig();
        localConfig.setSettings(Map.of("test", this.createMockSetting(null)));

        AwsSqsBatchProcessor processor = new AwsSqsBatchProcessor(
                new MockAwsBatchSubscriberFlow("test"),
                localConfig,
                this.task,
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
                            String value = record.getData().get(j).body();
                            assertThat(value, equalTo(Integer.toString(j)));
                        }
                    }
                }).verifyComplete();

        processor = new AwsSqsBatchProcessor(
                new MockAwsBatchSubscriberFlow("test"),
                localConfig,
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
                            String value = record.getData().get(j).body();
                            assertThat(value, equalTo(Integer.toString(j)));
                        }
                    }
                }).verifyComplete();
    }

    @Test
    void shouldRecordFailure() {
        AwsSqsConfig localConfig = new AwsSqsConfig();
        localConfig.setSettings(Map.of("test", this.createMockSetting(List.of())));

        AwsSqsBatchProcessor processor = new AwsSqsBatchProcessor(
                new MockAwsBatchSubscriberFlow("test"),
                localConfig,
                this.errorTask,
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

        processor = new AwsSqsBatchProcessor(
                new MockAwsBatchSubscriberFlow("test"),
                localConfig,
                this.errorTask,
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
        AwsSqsConfig localConfig = new AwsSqsConfig();
        localConfig.setSettings(Map.of("test", this.createMockSetting(List.of())));

        AwsSqsBatchProcessor processor = new AwsSqsBatchProcessor(
                new MockAwsBatchSubscriberFlow("test"),
                localConfig,
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

        processor = new AwsSqsBatchProcessor(
                new MockAwsBatchSubscriberFlow("test"),
                localConfig,
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
