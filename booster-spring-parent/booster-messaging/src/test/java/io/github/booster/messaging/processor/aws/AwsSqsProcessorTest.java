package io.github.booster.messaging.processor.aws;

import arrow.core.Option;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.messaging.config.AwsSqsConfig;
import io.github.booster.messaging.config.AwsSqsSetting;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.subscriber.aws.MockAwsSubscriberFlow;
import io.github.booster.task.Task;
import io.github.booster.task.impl.AsyncTask;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import kotlin.jvm.functions.Function1;
import lombok.val;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AwsSqsProcessorTest {

    private final Function1<Message, Mono<Message>> process =
            Mono::just;

    private final Function1<Throwable, Message> exceptionProcess =
            (throwable) -> {
                throw new IllegalStateException("error");
            };

    private final AwsSqsConfig awsSqsConfig = new AwsSqsConfig();

    private final Task<Message, Message> task =
            new AsyncTask<>(
                    "test",
                    Option.fromNullable(null),
                    Option.fromNullable(null),
                    Option.fromNullable(null),
                    new MetricsRegistry(new SimpleMeterRegistry()),
                    this.process,
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
                () -> new AwsSqsProcessor(
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
                () -> new AwsSqsProcessor(
                        new MockAwsSubscriberFlow("test"),
                        null,
                        this.task,
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsProcessor(
                        new MockAwsSubscriberFlow("test"),
                        new AwsSqsConfig(),
                        this.task,
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsProcessor(
                        new MockAwsSubscriberFlow("test"),
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
                new AwsSqsProcessor(
                        new MockAwsSubscriberFlow("test"),
                        this.awsSqsConfig,
                        this.task,
                        new OpenTelemetryConfig(null, "test"),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                ),
                notNullValue()
        );
        assertThat(
                new AwsSqsProcessor(
                        new MockAwsSubscriberFlow("test"),
                        this.awsSqsConfig,
                        this.task,
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                ),
                notNullValue()
        );
        assertThat(
                new AwsSqsProcessor(
                        new MockAwsSubscriberFlow("test"),
                        this.awsSqsConfig,
                        this.task,
                        new OpenTelemetryConfig(null, "test"),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        true
                ),
                notNullValue()
        );
        assertThat(
                new AwsSqsProcessor(
                        new MockAwsSubscriberFlow("test"),
                        this.awsSqsConfig,
                        this.task,
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        true
                ),
                notNullValue()
        );
    }

    @Test
    void shouldExecuteAndAcknowledge() {
        AwsSqsSetting mockSetting = mock(AwsSqsSetting.class);
        AwsSqsSetting.ReceiverSetting mockReceiverSetting = mock(AwsSqsSetting.ReceiverSetting.class);
        when(mockSetting.getReceiverSetting()).thenReturn(mockReceiverSetting);

        DeleteMessageResponse mockResponse = mock(DeleteMessageResponse.class);
        SdkHttpResponse mockSdkResponse = mock(SdkHttpResponse.class);
        when(mockSdkResponse.isSuccessful()).thenReturn(true);

        when(mockResponse.sdkHttpResponse()).thenReturn(mockSdkResponse);

        SqsClient mockClient = mock(SqsClient.class);
        when(mockClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(mockResponse);
        when(mockSetting.createClient()).thenReturn(mockClient);

        AwsSqsConfig localConfig = new AwsSqsConfig();
        localConfig.setSettings(Map.of("test", mockSetting));

        AwsSqsProcessor processor = new AwsSqsProcessor(
                        new MockAwsSubscriberFlow("test"),
                        localConfig,
                        this.task,
                        new OpenTelemetryConfig(null, "test"),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        true
                );

        StepVerifier.create(processor.process().take(5).collectList())
                .consumeNextWith(list -> {
                    assertThat(list, hasSize(5));
                    for (int i = 0; i < list.size(); i++) {
                        assertThat(list.get(i).isRight(), is(true));
                        val record = list.get(i).getOrNull();
                        assertThat(record, notNullValue());
                        assertThat(record.isAcknowledged(), is(true));
                        assertThat(record.getData().messageId(), equalTo(Integer.toString(i)));
                    }
                }).verifyComplete();

        processor = new AwsSqsProcessor(
                new MockAwsSubscriberFlow("test"),
                localConfig,
                this.task,
                new OpenTelemetryConfig(null, "test"),
                new MetricsRegistry(new SimpleMeterRegistry()),
                false
        );

        StepVerifier.create(processor.process().take(5).collectList())
                .consumeNextWith(list -> {
                    assertThat(list, hasSize(5));
                    for (int i = 0; i < list.size(); i++) {
                        assertThat(list.get(i).isRight(), is(true));
                        val record = list.get(i).getOrNull();
                        assertThat(record, notNullValue());
                        assertThat(record.isAcknowledged(), is(true));
                        assertThat(record.getData().messageId(), equalTo(Integer.toString(i)));
                    }
                }).verifyComplete();
    }

    @Test
    void shouldExecuteAndFailAcknowledge() {
        // the AwsSqsSetting by default will return an invalid SqsClient, which
        // means it will fail the delete message step.
        AwsSqsProcessor processor = new AwsSqsProcessor(
                new MockAwsSubscriberFlow("test"),
                this.awsSqsConfig,
                this.task,
                new OpenTelemetryConfig(null, "test"),
                new MetricsRegistry(new SimpleMeterRegistry()),
                true
        );

        StepVerifier.create(processor.process().take(5).collectList())
                .consumeNextWith(list -> {
                    assertThat(list, hasSize(5));
                    for (int i = 0; i < list.size(); i++) {
                        assertThat(list.get(i).isRight(), is(true));
                        val record = list.get(i).getOrNull();
                        assertThat(record, notNullValue());
                        assertThat(record.isAcknowledged(), is(false));
                        assertThat(record.getData().messageId(), equalTo(Integer.toString(i)));
                    }
                }).verifyComplete();

        processor = new AwsSqsProcessor(
                new MockAwsSubscriberFlow("test"),
                this.awsSqsConfig,
                this.task,
                new OpenTelemetryConfig(null, "test"),
                new MetricsRegistry(new SimpleMeterRegistry()),
                false
        );

        StepVerifier.create(processor.process().take(5).collectList())
                .consumeNextWith(list -> {
                    assertThat(list, hasSize(5));
                    for (int i = 0; i < list.size(); i++) {
                        assertThat(list.get(i).isRight(), is(true));
                        val record = list.get(i).getOrNull();
                        assertThat(record, notNullValue());
                        assertThat(record.isAcknowledged(), is(false));
                        assertThat(record.getData().messageId(), equalTo(Integer.toString(i)));
                    }
                }).verifyComplete();
    }
}
