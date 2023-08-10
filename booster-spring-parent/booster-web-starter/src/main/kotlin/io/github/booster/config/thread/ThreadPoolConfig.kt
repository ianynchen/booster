package io.github.booster.config.thread

import arrow.core.Option
import arrow.core.Option.Companion.fromNullable
import arrow.core.getOrElse
import io.github.booster.commons.cache.GenericKeyedObjectCache
import io.github.booster.commons.cache.KeyedCacheObjectFactory
import io.github.booster.commons.cache.KeyedObjectCache
import io.github.booster.commons.metrics.MetricsRegistry
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
class ThreadPoolConfig :
    KeyedCacheObjectFactory<String, ExecutorService>,
    KeyedObjectCache<String, ExecutorService>,
    ApplicationContextAware {

    private var settings: Map<String, ThreadPoolSetting> = mapOf()
    private var registry: Option<MetricsRegistry> = fromNullable(null)
    private var applicationContext: Option<ApplicationContext> = fromNullable(null)
    private var pool: KeyedObjectCache<String, ExecutorService>

    init {
        this.pool = GenericKeyedObjectCache(this)
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

    fun setMetricsRegistry(registry: MetricsRegistry?) {
        this.registry = fromNullable(registry)
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = fromNullable(applicationContext)
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
