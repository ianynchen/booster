package io.github.booster.messaging.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Auto-configuration class for Kafka subscribers.
 */
public class KafkaSubscriberConfig {

    private Map<String, KafkaSubscriberSetting> settings = new HashMap<>();

    /**
     * Default constructor
     */
    public KafkaSubscriberConfig() {
    }

    /**
     * Get all settings.
     * @return a map of subscriber setting identified by name of subscriber.
     */
    public Map<String, KafkaSubscriberSetting> getSettings() {
        return settings;
    }

    /**
     * Let spring inject the settings
     * @param settings a map of subscriber name and corresponding settings.
     */
    public void setSettings(Map<String, KafkaSubscriberSetting> settings) {
        this.settings = settings == null ? Map.of() : settings;
    }

    /**
     * Retrieves a {@link KafkaSubscriberSetting} by name, if not present, return null
     * @param name name of subscriber in interest.
     * @return {@link KafkaSubscriberSetting} or null if not present.
     */
    public KafkaSubscriberSetting getSetting(String name) {

        if (name == null) {
            return null;
        }
        return this.settings.get(name);
    }
}
