package io.github.booster.messaging.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for GCP pub/sub subscribers.
 */
public class GcpPubSubSubscriberConfig {

    private Map<String, GcpPubSubSubscriberSetting> settings = new HashMap<>();

    /**
     * Allows auto-configuration injection
     * @param settings a map of name and setting pair for each subscriber. Key is the name of subscriber
     */
    public void setSettings(
            Map<String, GcpPubSubSubscriberSetting> settings
    ) {
        this.settings = settings == null ? Map.of() : settings;
    }

    /**
     * Retrieves named subscriber setting, null if doesn't exist.
     * @param name name of the subscriber
     * @return {@link GcpPubSubSubscriberSetting}
     */
    public GcpPubSubSubscriberSetting getSetting(String name) {
        if (name == null) {
            return null;
        }
        return this.settings.get(name);
    }
}
