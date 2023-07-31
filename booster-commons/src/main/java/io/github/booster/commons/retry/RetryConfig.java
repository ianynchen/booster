package io.github.booster.commons.retry;

import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.pool.NamedObjectPool;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides a central repository for {@link Retry} management.
 */
public class RetryConfig extends NamedObjectPool<Retry> {

    private static final Logger log = LoggerFactory.getLogger(RetryConfig.class);

    private Map<String, RetrySetting> settings;

    private MetricsRegistry registry;

    /**
     * Default constructor
     */
    public RetryConfig() {
        this(null);
    }

    /**
     * Constructor with default retry settings.
     * @param settings map of {@link RetrySetting} identified by name
     */
    public RetryConfig(Map<String, RetrySetting> settings) {
        this.settings = settings == null ? new HashMap<>() : settings;
    }

    public void setSettings(Map<String, RetrySetting> settings) {
        this.settings = settings == null ? new HashMap<>() : settings;
    }

    public void setMetricsRegistry(MetricsRegistry registry) {
        this.registry = registry == null ?
                new MetricsRegistry() :
                registry;
    }

    @Override
    protected Retry createObject(String name) {
        log.debug("booster-commons - cache contains [{}] entry: {}", name, this.settings.containsKey(name));
        return this.settings.containsKey(name) ?
                this.settings.get(name).buildRetry(name, this.registry).orNull() :
                null;
    }
}
