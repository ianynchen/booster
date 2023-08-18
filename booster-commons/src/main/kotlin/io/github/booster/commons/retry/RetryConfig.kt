package io.github.booster.commons.retry

import io.github.booster.commons.cache.GenericKeyedObjectCache
import io.github.booster.commons.cache.KeyedCacheObjectFactory
import io.github.booster.commons.cache.KeyedObjectCache
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.resilience4j.retry.Retry
import org.slf4j.LoggerFactory

/**
 * Provides a central repository for [Retry] management.
 */
class RetryConfig @JvmOverloads constructor(settings: Map<String, RetrySetting>? = null) :
    KeyedCacheObjectFactory<String, Retry>, KeyedObjectCache<String, Retry> {

    private var settings: Map<String, RetrySetting>

    private var registry: MetricsRegistry? = null

    private var pool: GenericKeyedObjectCache<String, Retry>

    /**
     * Constructor with default retry settings.
     * @param settings map of [RetrySetting] identified by name
     */
    init {
        this.settings = settings ?: HashMap()
        this.pool = GenericKeyedObjectCache(this)
    }

    override fun create(key: String): Retry? {
        log.debug("booster-commons - cache contains [{}] entry: {}", key, settings.containsKey(key))
        return if (settings.containsKey(key))
            settings[key]!!.buildRetry(key, registry).orNull()
        else null
    }

    override fun get(key: String): Retry? = this.pool.get(key)

    fun setSettings(settings: Map<String, RetrySetting>?) {
        this.settings = settings ?: mapOf()
        this.pool = GenericKeyedObjectCache(this)
    }

    fun getSettings() = this.settings

    fun setMetricsRegistry(registry: MetricsRegistry?) {
        this.registry = registry ?: MetricsRegistry()
    }

    companion object {
        private val log = LoggerFactory.getLogger(RetryConfig::class.java)
    }

    override fun getKeys(): Set<String> = this.pool.getKeys()
}
