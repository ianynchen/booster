package io.github.booster.messaging.util.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.booster.commons.circuit.breaker.CircuitBreakerConfig;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.retry.RetryConfig;
import io.github.booster.config.thread.ThreadPoolConfig;
import io.github.booster.config.thread.ThreadPoolSetting;
import io.github.booster.factories.HttpClientFactory;
import io.github.booster.factories.TaskFactory;
import io.github.booster.http.client.config.HttpClientConnectionConfig;
import io.github.booster.messaging.config.AwsSqsConfig;
import io.github.booster.messaging.config.AwsSqsSetting;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.publisher.aws.AwsSqsPublisher;
import io.github.booster.messaging.subscriber.aws.AwsSqsSubscriber;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;

import java.util.Map;

public interface SqsUtil {

    static AwsSqsSetting createQueue(
            LocalStackContainer localstack,
            String queueName
    ) {
        AwsSqsSetting awsSqsSetting = new AwsSqsSetting();

        SqsClient client = SqsClient.builder()
                .endpointOverride(localstack.getEndpoint())
                .region(Region.of(localstack.getRegion()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        localstack.getAccessKey(),
                                        localstack.getSecretKey()
                                )
                        )
                ).build();
        CreateQueueResponse createQueueResponse = client.createQueue(
                CreateQueueRequest.builder()
                        .queueName(queueName)
                        .build()
        );

        awsSqsSetting.setQueueUrl(createQueueResponse.queueUrl());
        awsSqsSetting.setRegion(Region.of(localstack.getRegion()));
        awsSqsSetting.setCredentials(new AwsSqsSetting.AwsCredentials());
        awsSqsSetting.getCredentials().setAccessKey(localstack.getAccessKey());
        awsSqsSetting.getCredentials().setSecretKey(localstack.getSecretKey());
        client.close();

        return awsSqsSetting;
    }

    static <T> AwsSqsPublisher<T> createPublisher(
            AwsSqsConfig awsSqsConfig,
            String serviceName,
            ObjectMapper mapper
    ) {
        return new AwsSqsPublisher<>(
                serviceName,
                awsSqsConfig,
                new OpenTelemetryConfig(null, serviceName),
                new TaskFactory(
                        new ThreadPoolConfig(null, null),
                        new RetryConfig(),
                        new CircuitBreakerConfig(),
                        new HttpClientFactory(
                                new HttpClientConnectionConfig(null),
                                WebClient.builder(),
                                mapper == null ? new ObjectMapper() : mapper
                        ),
                        new MetricsRegistry(new SimpleMeterRegistry())
                ),
                mapper == null ? new ObjectMapper() : mapper,
                new MetricsRegistry(new SimpleMeterRegistry()),
                true
        );
    }

    static AwsSqsSubscriber createSubscriber(
            String name,
            AwsSqsConfig awsSqsConfig
    ) {
        ThreadPoolConfig threadPoolConfig = new ThreadPoolConfig(null, null);
        threadPoolConfig.setSettings(Map.of(name, new ThreadPoolSetting()));
        return new AwsSqsSubscriber(
                name,
                awsSqsConfig,
                threadPoolConfig,
                new MetricsRegistry(new SimpleMeterRegistry()),
                new OpenTelemetryConfig(null, name),
                true
        );
    }
}
