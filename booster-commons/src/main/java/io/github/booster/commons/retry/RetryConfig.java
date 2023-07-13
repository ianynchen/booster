package io.github.booster.commons.retry;

import arrow.core.Option;
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
public class RetryConfig extends NamedObjectPool<Option<Retry>> {

    private static Logger LOG = LoggerFactory.getLogger(RetryConfig.class);

    private Map<String, RetrySetting> retrySettings;

    private MetricsRegistry registry;

    /**
     * Default constructor
     */
    public RetryConfig() {
        this(null);
    }

    /**
     * Constructor with default retry settings.
     * @param retrySettings map of {@link RetrySetting} identified by name
     */
    public RetryConfig(Map<String, RetrySetting> retrySettings) {
        this.retrySettings = retrySettings == null ? new HashMap<>() : retrySettings;
    }

    public void setRetrySettings(Map<String, RetrySetting> retrySettings) {
        this.retrySettings = retrySettings == null ? new HashMap<>() : retrySettings;
    }

    public void setMetricsRegistry(MetricsRegistry registry) {
        this.registry = registry == null ?
                new MetricsRegistry() :
                registry;
    }

    @Override
    protected Option<Retry> createObject(String name) {
        return this.retrySettings.containsKey(name) ?
                this.retrySettings.get(name).buildRetry(name, this.registry) :
                Option.fromNullable(null);
    }

    @Override
    public Option<Option<Retry>> getOption(String name) {
        throw new UnsupportedOperationException("operation not supported");
    }
}
