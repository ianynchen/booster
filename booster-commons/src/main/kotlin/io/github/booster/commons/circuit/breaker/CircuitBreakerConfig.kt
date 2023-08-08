package io.github.booster.commons.circuit.breaker

import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.commons.pool.GenericKeyedObjectPool
import io.github.booster.commons.pool.KeyedObjectPool
import io.github.booster.commons.pool.KeyedPoolObjectFactory
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import org.slf4j.LoggerFactory

/**
 * Circuit breaker config objects that can be created as a Spring bean.
 * This object also caches circuit breakers created and returns cached value
 * if name is the same to avoid creating duplicate circuit breakers.
 */
class CircuitBreakerConfig @JvmOverloads constructor(settings: Map<String, CircuitBreakerSetting>? = null) :
    KeyedPoolObjectFactory<String, CircuitBreaker>, KeyedObjectPool<String, CircuitBreaker> {

    private var settings: Map<String, CircuitBreakerSetting> = mapOf()
    private var registry: MetricsRegistry? = null
    private lateinit var pool: KeyedObjectPool<String, CircuitBreaker>

    /**
     * Constructor with default settings.
     * @param settings [CircuitBreakerSetting] identified by name.
     */
    init {
        setSettings(settings)
    }

    override fun create(key: String): CircuitBreaker? {
        log.debug("booster-commons - cache contains [{}] entry: {}", key, settings.containsKey(key))
        return if (settings.containsKey(key)) {
            settings[key]!!.buildCircuitBreaker(key, registry).orNull()
        } else {
            null
        }
    }

    /**
     * Setter method for use as Spring configuration properties.
     * @param settings map of [CircuitBreakerSetting], key is the name for each setting.
     */
    fun setSettings(settings: Map<String, CircuitBreakerSetting>?) {
        this.settings = settings ?: mapOf()
        this.pool = GenericKeyedObjectPool(this)
    }

    /**
     * Sets [MetricsRegistry] object
     * @param registry [MetricsRegistry]
     */
    fun setMetricsRegistry(registry: MetricsRegistry?) {
        this.registry = registry ?: MetricsRegistry()
    }

    companion object {
        private val log = LoggerFactory.getLogger(CircuitBreakerConfig::class.java)
    }

    override fun get(key: String): CircuitBreaker? = this.pool[key]
}
