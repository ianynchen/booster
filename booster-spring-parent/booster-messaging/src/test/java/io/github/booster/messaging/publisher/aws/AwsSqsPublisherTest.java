package io.github.booster.messaging.publisher.aws;

import arrow.core.Option;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.booster.commons.circuit.breaker.CircuitBreakerConfig;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.retry.RetryConfig;
import io.github.booster.config.thread.ThreadPoolConfigGeneric;
import io.github.booster.factories.HttpClientFactory;
import io.github.booster.factories.TaskFactory;
import io.github.booster.http.client.config.HttpClientConnectionConfig;
import io.github.booster.messaging.config.AwsSqsConfig;
import io.github.booster.messaging.config.AwsSqsSetting;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.publisher.PublisherRecord;
import io.github.booster.messaging.util.aws.SqsUtil;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.Builder;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
class AwsSqsPublisherTest {

    @Builder
    @Data
    public static class TestData {
        private String name;
        private String value;
    }

    @Container
    public LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:latest")
    ).withServices(LocalStackContainer.Service.SQS);

    private AwsSqsConfig mockConfig;

    private SqsClient mockSqsClient;

    private AwsSqsSetting mockSetting;

    @BeforeEach
    void setup() {
        this.mockSetting = mock(AwsSqsSetting.class);

        this.mockConfig = mock(AwsSqsConfig.class);
        when(this.mockConfig.get(anyString())).thenReturn(this.mockSetting);

        this.mockSqsClient = mock(SqsClient.class);
        when(this.mockConfig.getClient(anyString())).thenReturn(Option.fromNullable(this.mockSqsClient));
    }

    @Test
    void shouldFailCreate() {

        AwsSqsConfig awsSqsConfig = new AwsSqsConfig();
        AwsSqsSetting setting = new AwsSqsSetting();
        setting.setRegion(Region.AF_SOUTH_1);
        setting.setQueueUrl("http://test");
        awsSqsConfig.setSettings(
                Map.of("abc", setting)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsPublisher<String>(
                        null,
                        awsSqsConfig,
                        new OpenTelemetryConfig(null, "test"),
                        new TaskFactory(
                                new ThreadPoolConfigGeneric(),
                                new RetryConfig(),
                                new CircuitBreakerConfig(),
                                new HttpClientFactory(
                                        new HttpClientConnectionConfig(null),
                                        WebClient.builder(),
                                        new ObjectMapper()
                                ),
                                new MetricsRegistry(new SimpleMeterRegistry())
                        ),
                        new ObjectMapper(),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsPublisher<String>(
                        "",
                        awsSqsConfig,
                        new OpenTelemetryConfig(null, "test"),
                        new TaskFactory(
                                new ThreadPoolConfigGeneric(),
                                new RetryConfig(),
                                new CircuitBreakerConfig(),
                                new HttpClientFactory(
                                        new HttpClientConnectionConfig(null),
                                        WebClient.builder(),
                                        new ObjectMapper()
                                ),
                                new MetricsRegistry(new SimpleMeterRegistry())
                        ),
                        new ObjectMapper(),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsPublisher<String>(
                        " ",
                        awsSqsConfig,
                        new OpenTelemetryConfig(null, "test"),
                        new TaskFactory(
                                new ThreadPoolConfigGeneric(),
                                new RetryConfig(),
                                new CircuitBreakerConfig(),
                                new HttpClientFactory(
                                        new HttpClientConnectionConfig(null),
                                        WebClient.builder(),
                                        new ObjectMapper()
                                ),
                                new MetricsRegistry(new SimpleMeterRegistry())
                        ),
                        new ObjectMapper(),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsPublisher<String>(
                        "abc",
                        null,
                        new OpenTelemetryConfig(null, "test"),
                        new TaskFactory(
                                new ThreadPoolConfigGeneric(),
                                new RetryConfig(),
                                new CircuitBreakerConfig(),
                                new HttpClientFactory(
                                        new HttpClientConnectionConfig(null),
                                        WebClient.builder(),
                                        new ObjectMapper()
                                ),
                                new MetricsRegistry(new SimpleMeterRegistry())
                        ),
                        new ObjectMapper(),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsPublisher<String>(
                        "abc",
                        new AwsSqsConfig(),
                        new OpenTelemetryConfig(null, "test"),
                        new TaskFactory(
                                new ThreadPoolConfigGeneric(),
                                new RetryConfig(),
                                new CircuitBreakerConfig(),
                                new HttpClientFactory(
                                        new HttpClientConnectionConfig(null),
                                        WebClient.builder(),
                                        new ObjectMapper()
                                ),
                                new MetricsRegistry(new SimpleMeterRegistry())
                        ),
                        new ObjectMapper(),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsPublisher<String>(
                        "abc",
                        awsSqsConfig,
                        new OpenTelemetryConfig(null, "test"),
                        null,
                        new ObjectMapper(),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new AwsSqsPublisher<String>(
                        "abc",
                        awsSqsConfig,
                        new OpenTelemetryConfig(null, "test"),
                        new TaskFactory(
                                new ThreadPoolConfigGeneric(),
                                new RetryConfig(),
                                new CircuitBreakerConfig(),
                                new HttpClientFactory(
                                        new HttpClientConnectionConfig(null),
                                        WebClient.builder(),
                                        new ObjectMapper()
                                ),
                                new MetricsRegistry(new SimpleMeterRegistry())
                        ),
                        new ObjectMapper(),
                        null,
                        false
                )
        );
    }

    @Test
    void shouldCreate() {

        AwsSqsConfig awsSqsConfig = new AwsSqsConfig();
        AwsSqsSetting setting = new AwsSqsSetting();
        setting.setRegion(Region.AF_SOUTH_1);
        setting.setQueueUrl("http://test");
        awsSqsConfig.setSettings(
                Map.of("abc", setting)
        );

        assertThat(
                new AwsSqsPublisher<String>(
                        "abc",
                        awsSqsConfig,
                        new OpenTelemetryConfig(null, "test"),
                        new TaskFactory(
                                new ThreadPoolConfigGeneric(),
                                new RetryConfig(),
                                new CircuitBreakerConfig(),
                                new HttpClientFactory(
                                        new HttpClientConnectionConfig(null),
                                        WebClient.builder(),
                                        new ObjectMapper()
                                ),
                                new MetricsRegistry(new SimpleMeterRegistry())
                        ),
                        new ObjectMapper(),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                ),
                notNullValue()
        );

        assertThat(
                new AwsSqsPublisher<String>(
                        "abc",
                        awsSqsConfig,
                        null,
                        new TaskFactory(
                                new ThreadPoolConfigGeneric(),
                                new RetryConfig(),
                                new CircuitBreakerConfig(),
                                new HttpClientFactory(
                                        new HttpClientConnectionConfig(null),
                                        WebClient.builder(),
                                        new ObjectMapper()
                                ),
                                new MetricsRegistry(new SimpleMeterRegistry())
                        ),
                        new ObjectMapper(),
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                ),
                notNullValue()
        );

        assertThat(
                new AwsSqsPublisher<String>(
                        "abc",
                        awsSqsConfig,
                        new OpenTelemetryConfig(null, "test"),
                        new TaskFactory(
                                new ThreadPoolConfigGeneric(),
                                new RetryConfig(),
                                new CircuitBreakerConfig(),
                                new HttpClientFactory(
                                        new HttpClientConnectionConfig(null),
                                        WebClient.builder(),
                                        new ObjectMapper()
                                ),
                                new MetricsRegistry(new SimpleMeterRegistry())
                        ),
                        null,
                        new MetricsRegistry(new SimpleMeterRegistry()),
                        false
                ),
                notNullValue()
        );
    }

    @Test
    void shouldPublish() {
        AwsSqsConfig awsSqsConfig = new AwsSqsConfig();
        AwsSqsSetting awsSqsSetting = SqsUtil.createQueue(
                this.localstack,
                "test-queue"
        );
        awsSqsConfig.setSettings(Map.of("test", awsSqsSetting));

        AwsSqsPublisher<String> publisher = SqsUtil.createPublisher(
                awsSqsConfig,
                "test",
                new ObjectMapper()
        );

        SqsRecord<String> record = new SqsRecord<>(
                Map.of(),
                "test"
        );
        StepVerifier.create(publisher.publish("abc", record))
                .consumeNextWith(either -> {
                    assertThat(either.isRight(), equalTo(true));

                    Option<PublisherRecord> recordOption = either.getOrNull();
                    assertThat(recordOption, notNullValue());
                    assertThat(recordOption.isDefined(), is(true));

                    PublisherRecord publisherRecord = recordOption.orNull();
                    assertThat(publisherRecord, notNullValue());
                    assertThat(publisherRecord.getRecordId(), notNullValue());
                }).verifyComplete();
    }

    @Test
    void shouldHandlePublishException() {
        AwsSqsSetting awsSqsSetting = SqsUtil.createQueue(
                this.localstack,
                "test-queue"
        );

        when(this.mockSqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(new IllegalArgumentException("error"));
        when(this.mockSetting.getQueueUrl()).thenReturn(awsSqsSetting.getQueueUrl());

        AwsSqsPublisher<String> publisher = SqsUtil.createPublisher(
                this.mockConfig,
                "test",
                new ObjectMapper()
        );

        SqsRecord<String> record = new SqsRecord<>(
                Map.of(),
                "test"
        );
        StepVerifier.create(publisher.publish("abc", record))
                .consumeNextWith(either -> {
                    assertThat(either.isLeft(), equalTo(true));
                    Throwable t = either.swap().getOrNull();
                    assertThat(t, notNullValue());
                    assertThat(t, instanceOf(IllegalArgumentException.class));
                    assertThat(t.getMessage(), equalTo("error"));
                }).verifyComplete();
    }

    @Test
    void shouldHandlePublishQuietError() {
        AwsSqsSetting awsSqsSetting = SqsUtil.createQueue(
                this.localstack,
                "test-queue"
        );

        SendMessageResponse mockResponse = mock(SendMessageResponse.class);
        when(mockResponse.messageId()).thenReturn(null);
        when(this.mockSqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(mockResponse);
        when(this.mockSetting.getQueueUrl()).thenReturn(awsSqsSetting.getQueueUrl());

        AwsSqsPublisher<String> publisher = SqsUtil.createPublisher(
                this.mockConfig,
                "test",
                new ObjectMapper()
        );

        SqsRecord<String> record = new SqsRecord<>(
                Map.of(),
                "test"
        );
        StepVerifier.create(publisher.publish("abc", record))
                .consumeNextWith(either -> {
                    assertThat(either.isLeft(), equalTo(true));
                    Throwable t = either.swap().getOrNull();
                    assertThat(t, notNullValue());
                    assertThat(t, instanceOf(IllegalStateException.class));
                    assertThat(t.getMessage(), equalTo("no response or no message ID from response"));
                }).verifyComplete();
    }

    @Test
    void shouldHandleSerializationError() throws JsonProcessingException {
        AwsSqsSetting awsSqsSetting = SqsUtil.createQueue(
                this.localstack,
                "test-queue"
        );

        SendMessageResponse mockResponse = mock(SendMessageResponse.class);
        when(mockResponse.messageId()).thenReturn(UUID.randomUUID().toString());
        when(this.mockSqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(mockResponse);
        when(this.mockSetting.getQueueUrl()).thenReturn(awsSqsSetting.getQueueUrl());
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        when(mockMapper.writeValueAsString(any()))
                .thenThrow(new IllegalArgumentException("error parsing"));

        AwsSqsPublisher<TestData> publisher = SqsUtil.createPublisher(
                this.mockConfig,
                "test",
                mockMapper
        );

        SqsRecord<TestData> record = new SqsRecord<>(
                Map.of(),
                TestData.builder().name("abc").value("abc").build()
        );

        StepVerifier.create(publisher.publish("abc", record))
                .consumeNextWith(either -> {
                    assertThat(either.isLeft(), equalTo(true));
                    Throwable t = either.swap().getOrNull();
                    assertThat(t, notNullValue());
                    assertThat(t, instanceOf(IllegalArgumentException.class));
                    assertThat(t.getMessage(), equalTo("error parsing"));
                }).verifyComplete();
    }
}
