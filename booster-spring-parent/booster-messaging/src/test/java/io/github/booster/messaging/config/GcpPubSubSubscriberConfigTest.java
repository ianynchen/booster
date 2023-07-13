package io.github.booster.messaging.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

class GcpPubSubSubscriberConfigTest {

    @Test
    void shouldHandleDefaults() {
        GcpPubSubSubscriberConfig config = new GcpPubSubSubscriberConfig();
        assertThat(config, notNullValue());
        assertThat(config.getSetting("abc"), nullValue());
        assertThat(config.getSetting(null), nullValue());
    }

    @Test
    void shouldHandleNullSetting() {
        GcpPubSubSubscriberConfig config = new GcpPubSubSubscriberConfig();
        config.setSettings(null);
        assertThat(config, notNullValue());
        assertThat(config.getSetting("abc"), nullValue());
    }

    @Test
    void shouldHandleCustomSetting() {
        GcpPubSubSubscriberConfig config = new GcpPubSubSubscriberConfig();
        config.setSettings(
                Map.of(
                        "test", new GcpPubSubSubscriberSetting()
                )
        );
        assertThat(config, notNullValue());
        assertThat(config.getSetting("abc"), nullValue());
        assertThat(config.getSetting("test"), notNullValue());
    }
}
