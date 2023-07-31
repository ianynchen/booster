package io.github.booster.commons.circuit.breaker;

import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.pool.NamedObjectPool;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Circuit breaker config objects that can be created as a Spring bean.
 * This object also caches circuit breakers created and returns cached value
 * if name is the same to avoid creating duplicate circuit breakers.
 */
public class CircuitBreakerConfig extends NamedObjectPool<CircuitBreaker> {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreakerConfig.class);

    private Map<String, CircuitBreakerSetting> settings;

    private MetricsRegistry registry;

    /**
     * Default constructor
     */
    public CircuitBreakerConfig() {
        this(null);
    }

    /**
     * Constructor with default settings.
     * @param settings {@link CircuitBreakerSetting} identified by name.
     */
    public CircuitBreakerConfig(Map<String, CircuitBreakerSetting> settings) {
        this.setSettings(settings);
    }

    /**
     * Setter method for use as Spring configuration properties.
     * @param settings map of {@link CircuitBreakerSetting}, key is the name for each setting.
     */
    public void setSettings(Map<String, CircuitBreakerSetting> settings) {
        this.settings = settings == null ? new HashMap<>() : settings;
    }

    /**
     * Sets {@link MetricsRegistry} object
     * @param registry {@link MetricsRegistry}
     */
    public void setMetricsRegistry(MetricsRegistry registry) {
        this.registry = registry == null ?
                new MetricsRegistry() :
                registry;
    }

    @Override
    protected CircuitBreaker createObject(String name) {
        log.debug("booster-commons - cache contains [{}] entry: {}", name, this.settings.containsKey(name));
        return this.settings.containsKey(name) ?
                this.settings.get(name).buildCircuitBreaker(name, this.registry).orNull() :
                null;
    }
}
