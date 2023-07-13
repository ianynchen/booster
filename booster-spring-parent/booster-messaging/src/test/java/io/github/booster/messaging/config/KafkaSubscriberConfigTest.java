package io.github.booster.messaging.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

class KafkaSubscriberConfigTest {

    @Test
    void shouldCreate() {
        KafkaSubscriberConfig config = new KafkaSubscriberConfig();
        assertThat(config.getSettings(), notNullValue());
        assertThat(config.getSettings().entrySet(), hasSize(0));

        config.setSettings(null);
        assertThat(config.getSettings(), notNullValue());
        assertThat(config.getSettings().entrySet(), hasSize(0));

        config.setSettings(Map.of("test", new KafkaSubscriberSetting()));
        assertThat(config.getSettings(), notNullValue());
        assertThat(config.getSettings().entrySet(), hasSize(1));
        assertThat(config.getSetting(null), nullValue());
        assertThat(config.getSetting("abc"), nullValue());
        assertThat(config.getSetting("test"), notNullValue());
    }
}
