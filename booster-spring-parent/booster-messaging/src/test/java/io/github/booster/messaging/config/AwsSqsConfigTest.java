package io.github.booster.messaging.config;

import arrow.core.Option;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

class AwsSqsConfigTest {

    @Test
    void shouldCreateWithNull() {
        AwsSqsConfig config = new AwsSqsConfig();
        assertThat(config.get("test"), nullValue());
        assertThat(config.getClient("test").isDefined(), equalTo(false));

        config.setSettings(null);
        assertThat(config.get("test"), nullValue());
        assertThat(config.getClient("test").isDefined(), equalTo(false));

        assertThat(config.get(null), nullValue());
        assertThat(config.getClient(null), notNullValue());
        assertThat(config.getClient(null).isDefined(), equalTo(false));
    }

    @Test
    void shouldCreate() {
        AwsSqsConfig config = new AwsSqsConfig();

        AwsSqsSetting setting = new AwsSqsSetting();
        setting.setQueueUrl("http://test");
        setting.setRegion(Region.AF_SOUTH_1);
        config.setSettings(Map.of("test", setting));

        assertThat(config.get("test"), notNullValue());
        Option<SqsClient> clientOption = config.getClient("test");
        assertThat(clientOption.isDefined(), equalTo(true));
        assertThat(
                config.getClient("test").orNull(),
                sameInstance(clientOption.orNull())
        );
    }
}
