package io.github.booster.messaging.config;

import arrow.core.Option;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.HashMap;
import java.util.Map;

public class AwsSqsConfig {

    private Map<String, AwsSqsSetting> settings = new HashMap<>();

    private final Map<String, SqsClient> cachedClients = new HashMap<>();

    public void setSettings(Map<String, AwsSqsSetting> settings) {
        this.settings = settings == null ? Map.of() : settings;
    }

    public AwsSqsSetting get(String name) {
        if (name == null) {
            return null;
        }
        return this.settings.get(name);
    }

    public Option<SqsClient> getClient(String name) {
        synchronized (this.cachedClients) {
            if (StringUtils.isBlank(name)) {
                return Option.fromNullable(null);
            } else if (this.cachedClients.containsKey(name)) {
                return Option.fromNullable(this.cachedClients.get(name));
            } else if (this.settings.containsKey(name)){
                SqsClient client = this.settings.get(name).createClient();
                this.cachedClients.put(name, client);
                return Option.fromNullable(client);
            } else {
                return Option.fromNullable(null);
            }
        }
    }
}
