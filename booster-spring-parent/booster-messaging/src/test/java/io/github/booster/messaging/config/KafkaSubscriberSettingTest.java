package io.github.booster.messaging.config;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class KafkaSubscriberSettingTest {

    @Test
    void shouldHaveValue() {
        KafkaSubscriberSetting setting = new KafkaSubscriberSetting();
        assertThat(setting.getQueueSize(), equalTo(1));
        setting.setQueueSize(-1);
        assertThat(setting.getQueueSize(), equalTo(1));
        setting.setQueueSize(2);
        assertThat(setting.getQueueSize(), equalTo(2));
    }
}
