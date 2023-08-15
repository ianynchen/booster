package io.github.booster.config.thread

import arrow.core.Option
import arrow.core.Option.Companion.fromNullable
import arrow.core.getOrElse
import io.github.booster.commons.cache.GenericKeyedObjectCache
import io.github.booster.commons.cache.KeyedCacheObjectFactory
import io.github.booster.commons.cache.KeyedObjectCache
import io.github.booster.commons.metrics.MetricsRegistry
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.cloud.sleuth.instrument.async.LazyTraceThreadPoolTaskExecutor
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ExecutorService

/**
 * Spring Configuration for thread groups.
 */
class ThreadPoolConfig(
    applicationContext: ApplicationContext?,
    registry: MetricsRegistry?
) :
    KeyedCacheObjectFactory<String, ExecutorService>,
    KeyedObjectCache<String, ExecutorService> {

    private var settings: Map<String, ThreadPoolSetting>
    private var pool: KeyedObjectCache<String, ExecutorService>

    private val registry: Option<MetricsRegistry>
    private val applicationContext: Option<ApplicationContext>

    init {
        this.pool = GenericKeyedObjectCache(this)
        this.applicationContext = fromNullable(applicationContext)
        this.registry = fromNullable(registry)
        this.settings = mapOf()
    }

    /**
     * Creates default thread pools upon start up.
     * @param settings thread pool settings.
     */
    fun setSettings(settings: Map<String, ThreadPoolSetting>?) {
        this.settings = settings ?: mapOf()
        this.pool = GenericKeyedObjectCache(this)
    }

    /**
     * Gets all settings
     * @return map of setting name and settings.
     */
    fun getSettings() = this.settings

    /**
     * Retrieves original setting.
     * @param name name of the setting.
     * @return [ThreadPoolSetting] if exists, otherwise null.
     */
    fun getSetting(name: String?): ThreadPoolSetting? {
        return if (name != null) {
            settings[name]
        } else null
    }

    @PreDestroy
    fun destroy() {
        this.pool.getKeys().forEach { key: String ->
            val threadPool = this.pool[key]!!
            threadPool.shutdown()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ThreadPoolConfig::class.java)
    }

    override fun create(key: String): ExecutorService? {
        if (settings.containsKey(key) && this.settings.get(key) != null) {
            val setting = this.settings[key]!!.validate(key)
            val executor = ThreadPoolTaskExecutor()
            log.debug("booster-starter - creating thread pool for [{}], setting: [{}]", key, setting)

            executor.corePoolSize = setting.coreSize
            executor.maxPoolSize = setting.maxSize
            executor.queueCapacity = setting.queueSize
            executor.threadNamePrefix = setting.prefix
            executor.initialize()

            return this.applicationContext.map {
                val taskExecutor = LazyTraceThreadPoolTaskExecutor(
                    it.autowireCapableBeanFactory,
                    executor
                )

                this.registry.flatMap { reg ->
                    reg.measureExecutorService(
                        fromNullable(taskExecutor.threadPoolExecutor),
                        key
                    )
                }.getOrElse {
                    taskExecutor.threadPoolExecutor
                }
            }.getOrElse {
                this.registry.flatMap { reg ->
                    reg.measureExecutorService(
                        fromNullable(executor.threadPoolExecutor),
                        key
                    )
                }.getOrElse {
                    executor.threadPoolExecutor
                }
            }
        }
        log.debug("booster-starter - no thread pool setup for [{}]", key)
        return null
    }

    override fun getKeys(): Set<String> = this.pool.getKeys()

    override fun get(key: String): ExecutorService? = this.pool[key]
}
