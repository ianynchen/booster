package io.github.booster.example.fulfillment.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.cloud.spring.core.DefaultGcpProjectIdProvider;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.cloud.spring.pubsub.core.PubSubConfiguration;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.DefaultPublisherFactory;
import com.google.cloud.spring.pubsub.support.DefaultSubscriberFactory;
import com.google.cloud.spring.pubsub.support.PublisherFactory;
import com.google.cloud.spring.pubsub.support.SubscriberFactory;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.TopicName;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@Getter
public class GcpPubsubConfig {

    private static final Logger log = LoggerFactory.getLogger(GcpPubsubConfig.class);

    private final String endpoint;

    private final String projectId;

    private final String topic;

    private final String subscription;

    private final boolean create;

    @Autowired
    public GcpPubsubConfig(
            @Value("${booster.gcp.pubsub.endpoint}")
            String endpoint,
            @Value("${booster.gcp.pubsub.projectId}")
            String projectId,
            @Value("${booster.gcp.pubsub.topic}")
            String topic,
            @Value("${booster.gcp.pubsub.subscription}")
            String subscription,
            @Value("${booster.gcp.pubsub.create:false}")
            boolean create
    ) {
        this.endpoint = endpoint;
        this.projectId = projectId;
        this.topic = topic;
        this.subscription = subscription;
        this.create = create;
    }

    private void createTopic(
            String topicId,
            TransportChannelProvider channelProvider,
            CredentialsProvider credentialsProvider
    ) throws IOException {
        log.info("fulfillment service - creating pub/sub topic [{}]", topicId);
        TopicAdminSettings topicAdminSettings = TopicAdminSettings.newBuilder()
                .setTransportChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build();
        try (TopicAdminClient topicAdminClient = TopicAdminClient.create(topicAdminSettings)) {
            TopicName topicName = TopicName.of(this.projectId, topicId);
            topicAdminClient.createTopic(topicName);
            log.info("fulfillment service - pub/sub topic [{}] created for project {}", topicId, this.projectId);
        }
    }

    private void createSubscription(
            String subscriptionId,
            String topicId,
            TransportChannelProvider channelProvider,
            CredentialsProvider credentialsProvider
    ) throws IOException {
        log.info("creating subscription: {}", subscriptionId);
        SubscriptionAdminSettings subscriptionAdminSettings = SubscriptionAdminSettings.newBuilder()
                .setTransportChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build();
        SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create(subscriptionAdminSettings);
        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(this.projectId, subscriptionId);
        subscriptionAdminClient.createSubscription(subscriptionName, TopicName.of(this.projectId, topicId), PushConfig.getDefaultInstance(), 10);

        log.info("subscription {} created for topic: {}, project: {}", subscriptionId, topicId, this.projectId);
    }

    @Bean
    public TransportChannelProvider transportChannelProvider(CredentialsProvider credentialsProvider) throws IOException {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(this.endpoint).usePlaintext().build();
        TransportChannelProvider transportChannelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));

        if (this.create) {
            createTopic(this.topic, transportChannelProvider, credentialsProvider);
            createSubscription(this.subscription, this.topic, transportChannelProvider, credentialsProvider);
        }
        return transportChannelProvider;
    }

    @Bean
    public GcpProjectIdProvider projectIdProvider() {
        return new DefaultGcpProjectIdProvider() {
            @Override
            public String getProjectId() {
                return projectId;
            }
        };
    }

    @Bean
    public CredentialsProvider credentialsProvider() {
        return NoCredentialsProvider.create();
    }

    @Bean
    public DefaultPublisherFactory publisherFactory(
            GcpProjectIdProvider projectIdProvider,
            TransportChannelProvider transportChannelProvider,
            CredentialsProvider credentialsProvider
    ) {
        final DefaultPublisherFactory defaultPublisherFactory = new DefaultPublisherFactory(projectIdProvider);
        defaultPublisherFactory.setCredentialsProvider(credentialsProvider);
        defaultPublisherFactory.setChannelProvider(transportChannelProvider);
        return defaultPublisherFactory;
    }

    @Bean
    public DefaultSubscriberFactory subscriberFactory(
            GcpProjectIdProvider projectIdProvider,
            TransportChannelProvider transportChannelProvider,
            CredentialsProvider credentialsProvider
    ) {
        PubSubConfiguration config = new PubSubConfiguration();
        config.initialize(projectIdProvider.getProjectId());
        final DefaultSubscriberFactory defaultSubscriberFactory = new DefaultSubscriberFactory(projectIdProvider, config);
        defaultSubscriberFactory.setCredentialsProvider(credentialsProvider);
        defaultSubscriberFactory.setChannelProvider(transportChannelProvider);
        return defaultSubscriberFactory;
    }

    @Bean
    public PubSubTemplate pubSubTemplate(
            PublisherFactory publisherFactory,
            SubscriberFactory subscriberFactory,
            CredentialsProvider credentialsProvider
    ) {
        if (publisherFactory instanceof DefaultPublisherFactory) {
            ((DefaultPublisherFactory) publisherFactory).setCredentialsProvider(credentialsProvider);
        }
        log.info("pubsub template created");
        return new PubSubTemplate(publisherFactory, subscriberFactory);
    }
}
