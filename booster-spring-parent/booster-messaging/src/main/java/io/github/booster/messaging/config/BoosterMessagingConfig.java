package io.github.booster.messaging.config;

import io.opentelemetry.api.OpenTelemetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This is auto-configuration class to create message
 * subscribers.
 */
@Configuration
public class BoosterMessagingConfig {

    /**
     * Default constructor
     */
    public BoosterMessagingConfig() {
    }

    /**
     * GCP pub/sub configuration bean
     * @return {@link GcpPubSubSubscriberConfig}
     */
    @Bean
    @ConfigurationProperties(prefix = "booster.messaging.gcp.subscriber")
    public GcpPubSubSubscriberConfig gcpPubSubSubscriberConfig() {
        return new GcpPubSubSubscriberConfig();
    }

    /**
     * Kafka configuration bean
     * @return {@link KafkaSubscriberConfig}
     */
    @Bean
    @ConfigurationProperties(prefix = "booster.messaging.kafka.subscriber")
    public KafkaSubscriberConfig kafkaSubscriberConfig() {
        return new KafkaSubscriberConfig();
    }

    /**
     * Creates an {@link OpenTelemetryConfig} bean
     * @param openTelemetry nullable {@link OpenTelemetry} object
     * @param serviceName name of the service to be used
     * @return {@link OpenTelemetryConfig} bean
     */
    @Bean
    public OpenTelemetryConfig openTelemetryConfig(
            @Autowired(required = false)
            OpenTelemetry openTelemetry,
            @Value("${spring.application.name:default-service}")
            String serviceName
    ) {
        return new OpenTelemetryConfig(openTelemetry, serviceName);
    }
}
