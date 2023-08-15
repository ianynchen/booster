package io.github.booster.messaging.subscriber.aws;

import arrow.core.Option;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.config.thread.ThreadPoolConfig;
import io.github.booster.config.thread.ThreadPoolSetting;
import io.github.booster.messaging.config.AwsSqsConfig;
import io.github.booster.messaging.config.AwsSqsSetting;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.publisher.aws.AwsSqsPublisher;
import io.github.booster.messaging.publisher.aws.SqsRecord;
import io.github.booster.messaging.util.aws.SqsUtil;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
class AwsSqsSubscriberTest {

    private static final Logger log = LoggerFactory.getLogger(AwsSqsSubscriberTest.class);

    @Container
    public LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:latest")
    ).withServices(LocalStackContainer.Service.SQS);

    private String queueUrl;

    private final AwsSqsConfig awsSqsConfig = new AwsSqsConfig();

    private final MetricsRegistry registry = new MetricsRegistry(new SimpleMeterRegistry());

    @BeforeEach
    void setup() {
        AwsSqsSetting setting = SqsUtil.createQueue(this.localstack, "test-queue");
        this.queueUrl = setting.getQueueUrl();
        AwsSqsSetting newSetting = new AwsSqsSetting();
        newSetting.setRegion(Region.of(this.localstack.getRegion()));

        AwsSqsSetting.AwsCredentials credentials = new AwsSqsSetting.AwsCredentials();
        credentials.setSecretKey(this.localstack.getSecretKey());
        credentials.setAccessKey(this.localstack.getAccessKey());
        newSetting.setCredentials(credentials);

        newSetting.setQueueUrl(this.queueUrl);
        newSetting.setReceiverSetting(new AwsSqsSetting.ReceiverSetting());
        newSetting.getReceiverSetting().setMaxNumberOfMessages(5);

        this.awsSqsConfig.setSettings(Map.of("test", newSetting));
    }

    @Test
    void shouldFailCreate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsSubscriber(
                        null,
                        this.awsSqsConfig,
                        new ThreadPoolConfig(),
                        this.registry,
                        new OpenTelemetryConfig(null, "test"),
                        true
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsSubscriber(
                        "",
                        this.awsSqsConfig,
                        new ThreadPoolConfig(),
                        this.registry,
                        new OpenTelemetryConfig(null, "test"),
                        true
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsSubscriber(
                        " ",
                        this.awsSqsConfig,
                        new ThreadPoolConfig(),
                        this.registry,
                        new OpenTelemetryConfig(null, "test"),
                        true
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsSubscriber(
                        "test",
                        null,
                        new ThreadPoolConfig(),
                        this.registry,
                        new OpenTelemetryConfig(null, "test"),
                        true
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsSubscriber(
                        "test",
                        new AwsSqsConfig(),
                        new ThreadPoolConfig(),
                        this.registry,
                        new OpenTelemetryConfig(null, "test"),
                        true
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsSubscriber(
                        "test",
                        new AwsSqsConfig(),
                        null,
                        this.registry,
                        new OpenTelemetryConfig(null, "test"),
                        true
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsSubscriber(
                        "test",
                        new AwsSqsConfig(),
                        new ThreadPoolConfig(),
                        null,
                        new OpenTelemetryConfig(null, "test"),
                        true
                )
        );
    }

    @Test
    void shouldCreate() {
        assertThat(
                SqsUtil.createSubscriber(
                        "test",
                        this.awsSqsConfig
                ),
                notNullValue()
        );
    }

    @Test
    void shouldReceiveBatch() {
        SqsRecord<String> record = SqsRecord.createMessage(
                Map.of("header1", "value1", "header2", "value2"),
                "test-content"
        );

        AwsSqsPublisher<String> publisher = SqsUtil.createPublisher(
                this.awsSqsConfig,
                "test",
                new ObjectMapper()
        );

        StepVerifier.create(publisher.publish("test-queue", record))
                .consumeNextWith(either -> {
                    assertThat(either.isRight(), equalTo(true));
                    assertThat(either.getOrNull(), notNullValue());
                    assertThat(either.getOrNull().orNull(), notNullValue());
                    assertThat(either.getOrNull().orNull().getRecordId(), notNullValue());
                }).verifyComplete();

        AwsSqsSubscriber subscriber = SqsUtil.createSubscriber(
                "test",
                this.awsSqsConfig
        );

        StepVerifier.create(subscriber.flux().take(1))
                .consumeNextWith(messages -> {
                    log.warn("start verification with messages: [{}]", messages);
                    assertThat(messages, notNullValue());
                    assertThat(messages, hasSize(1));
                    assertThat(messages.get(0).messageId(), notNullValue());
                    assertThat(messages.get(0).body(), equalTo("test-content"));
                    assertThat(messages.get(0).messageAttributes().keySet(), hasSize(2));
                    assertThat(messages.get(0).messageAttributes().keySet(), containsInAnyOrder("header1", "header2"));
                    assertThat(
                            messages.get(0).messageAttributes()
                                    .values()
                                    .stream()
                                    .map(MessageAttributeValue::stringValue)
                                    .collect(Collectors.toList()),
                            containsInAnyOrder("value1", "value2")
                    );
                }).verifyComplete();

        subscriber.stop();
    }

    @Test
    void shouldReceive() {
        SqsRecord<String> record = SqsRecord.createMessage(
                Map.of("header1", "value1", "header2", "value2"),
                "test-content"
        );

        AwsSqsPublisher<String> publisher = SqsUtil.createPublisher(
                this.awsSqsConfig,
                "test",
                new ObjectMapper()
        );

        StepVerifier.create(publisher.publish("test-queue", record))
                .consumeNextWith(either -> {
                    assertThat(either.isRight(), equalTo(true));
                    assertThat(either.getOrNull(), notNullValue());
                    assertThat(either.getOrNull().orNull(), notNullValue());
                    assertThat(either.getOrNull().orNull().getRecordId(), notNullValue());
                }).verifyComplete();

        AwsSqsSubscriber subscriber = SqsUtil.createSubscriber(
                "test",
                this.awsSqsConfig
        );

        StepVerifier.create(subscriber.flatFlux().take(1))
                .consumeNextWith(message -> {
                    log.warn("start verification with message: [{}]", message);
                    assertThat(message, notNullValue());
                    assertThat(message.messageId(), notNullValue());
                    assertThat(message.body(), equalTo("test-content"));
                    assertThat(message.messageAttributes().keySet(), hasSize(2));
                    assertThat(message.messageAttributes().keySet(), containsInAnyOrder("header1", "header2"));
                    assertThat(
                            message.messageAttributes()
                                    .values()
                                    .stream()
                                    .map(MessageAttributeValue::stringValue)
                                    .collect(Collectors.toList()),
                            containsInAnyOrder("value1", "value2")
                    );
                }).verifyComplete();
        
        subscriber.stop();
    }

    @Test
    void shouldHandleErrorFromSQS() {
        AwsSqsSetting mockSetting = mock(AwsSqsSetting.class);

        AwsSqsConfig mockConfig = mock(AwsSqsConfig.class);
        when(mockConfig.get(anyString())).thenReturn(mockSetting);

        SqsClient mockSqsClient = mock(SqsClient.class);
        when(mockConfig.getClient(anyString())).thenReturn(Option.fromNullable(mockSqsClient));
        when(mockSqsClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenThrow(new IllegalArgumentException("forced error"));
        when(mockSetting.getQueueUrl()).thenReturn(this.queueUrl);
        when(mockSetting.getReceiverSetting()).thenReturn(new AwsSqsSetting.ReceiverSetting());

        ThreadPoolConfig threadPoolConfig = new ThreadPoolConfig();
        threadPoolConfig.setSettings(Map.of("test", new ThreadPoolSetting(null, null, null, null)));
        AwsSqsSubscriber subscriber = new AwsSqsSubscriber(
                "test",
                mockConfig,
                threadPoolConfig,
                new MetricsRegistry(new SimpleMeterRegistry()),
                new OpenTelemetryConfig(null, "test"),
                true
        );

        StepVerifier.create(subscriber.flatFlux().take(1))
                .expectSubscription()
                .expectNoEvent(Duration.ofSeconds(5L))
                .thenCancel();
    }
}
