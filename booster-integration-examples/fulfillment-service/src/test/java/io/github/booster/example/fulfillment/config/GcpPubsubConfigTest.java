package io.github.booster.example.fulfillment.config;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.sleuth.autoconfig.instrument.kafka.TracingReactorKafkaAutoConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.testcontainers.containers.PubSubEmulatorContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;


@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        classes = {
                GcpPubsubConfig.class
        },
        properties = {
                "booster.gcp.pubsub.create=true"
        }
)
@EnableAutoConfiguration(exclude = TracingReactorKafkaAutoConfiguration.class)
@DirtiesContext
class GcpPubsubConfigTest {

    private static final Logger log = LoggerFactory.getLogger(GcpPubsubConfigTest.class);

    @Autowired
    private PubSubTemplate template;

    @Autowired
    private GcpPubsubConfig pubsubConfig;

    @Container
    public static PubSubEmulatorContainer emulator = new PubSubEmulatorContainer(
            DockerImageName.parse("gcr.io/google.com/cloudsdktool/google-cloud-cli:380.0.0-emulators")
                    .asCompatibleSubstituteFor("gcr.io/google.com/cloudsdktool/cloud-sdk")
    );

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("booster.gcp.pubsub.endpoint", () -> emulator.getEmulatorEndpoint());
    }

    @Test
    void shouldSend() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean published = new AtomicBoolean(false);
        ListenableFuture<String> result = template.publish(
                this.pubsubConfig.getTopic(),
                "abc"
        );

        result.addCallback(new ListenableFutureCallback<String>() {
            @Override
            public void onFailure(Throwable ex) {
                log.error("message publish to pub/sub failed", ex);
                latch.countDown();
            }

            @Override
            public void onSuccess(String result) {
                log.info("message published to pub/sub with result: {}", result);
                published.set(true);
                latch.countDown();
            }
        });

        latch.await(2L, TimeUnit.SECONDS);
        assertThat(published.get(), equalTo(true));

        List<AcknowledgeablePubsubMessage> messages = this.template.pull(
                this.pubsubConfig.getSubscription(),
                10,
                true
        );
        assertThat(messages, hasSize(1));
        String msg = messages.get(0).getPubsubMessage().getData().toString(StandardCharsets.UTF_8);
        log.info("message received: {}", msg);
        assertThat(msg, equalTo("abc"));
    }
}
