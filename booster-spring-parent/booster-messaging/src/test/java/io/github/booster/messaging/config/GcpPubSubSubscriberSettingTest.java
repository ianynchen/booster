package io.github.booster.messaging.config;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class GcpPubSubSubscriberSettingTest {

    @Test
    void shouldTakeDefaultValues() {
        GcpPubSubSubscriberSetting setting = new GcpPubSubSubscriberSetting();
        assertThat(setting, notNullValue());
    }

    @Test
    void shouldTakeCustomValue() {
        GcpPubSubSubscriberSetting setting = new GcpPubSubSubscriberSetting();
        setting.setSubscription("sub");
        setting.setMaxRecords(1000);
        assertThat(setting.getSubscription(), is("sub"));
        assertThat(setting.getMaxRecords(), is(1000));
    }
}
