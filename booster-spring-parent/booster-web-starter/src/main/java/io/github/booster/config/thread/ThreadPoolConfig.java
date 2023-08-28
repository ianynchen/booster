package io.github.booster.config.thread;

import arrow.core.Option;
import io.github.booster.commons.cache.GenericKeyedObjectCache;
import io.github.booster.commons.cache.KeyedCacheObjectFactory;
import io.github.booster.commons.cache.KeyedObjectCache;
import io.github.booster.commons.metrics.MetricsRegistry;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.sleuth.instrument.async.LazyTraceThreadPoolTaskExecutor;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Spring Configuration for thread groups.
 */
public class ThreadPoolConfig
        implements KeyedCacheObjectFactory<String, ExecutorService>,
        KeyedObjectCache<String, ExecutorService> {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolConfig.class);

    private Map<String, ThreadPoolSetting> settings = new HashMap<>();

    private KeyedObjectCache<String, ExecutorService> cache;

    private final MetricsRegistry registry;

    private final ApplicationContext applicationContext;

    /**
     * Constructs a {@link ThreadPoolConfig} object
     * @param applicationContext {@link ApplicationContext} to create {@link LazyTraceThreadPoolTaskExecutor}
     * @param registry {@link MetricsRegistry} to record metrics
     */
    public ThreadPoolConfig(
            ApplicationContext applicationContext,
            MetricsRegistry registry
    ) {
        this.applicationContext = applicationContext;
        this.registry = registry;
        this.cache = new GenericKeyedObjectCache<>(this);
    }

    /**
     * Creates default thread pools upon start up.
     * @param settings thread pool settings.
     */
    public void setSettings(Map<String, ThreadPoolSetting> settings) {
        this.settings = settings == null ? Map.of() : settings;
    }

    /**
     * Retrieves original setting.
     * @param key name of the setting.
     * @return {@link ThreadPoolSetting} if exists, otherwise null.
     */
    public ThreadPoolSetting getSetting(String key) {
        if (key != null) {
            return this.settings.get(key);
        }
        return null;
    }

    /**
     * Shuts down all threads when application closes.
     */
    @PreDestroy
    public void destroy() {
        this.cache.getKeys()
                .forEach(key -> {
                    ExecutorService threadPool = this.cache.get(key);
                    if (threadPool != null) {
                        threadPool.shutdown();
                    }
                });
    }

    /**
     * Creates an {@link ExecutorService}. {@link ExecutorService} created will
     * be instrumented by micrometer for metrics reporting if {@link io.micrometer.core.instrument.MeterRegistry}
     * instance is available.
     * @param key key of the thread pool to be created.
     * @return {@link ExecutorService} instance created, or null if no {@link ThreadPoolSetting} with
     *         the specified key can be found.
     */
    @Nullable
    @Override
    public ExecutorService create(String key) {
        if (this.settings.containsKey(key)) {
            ThreadPoolSetting setting = this.settings.get(key);
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            log.debug("booster-starter - creating thread pool for [{}], setting: [{}]", key, setting);

            if (StringUtils.isNotBlank(setting.getPrefix())) {
                setting.setPrefix(key);
            }
            if (setting.getCoreSize() > setting.getMaxSize()) {
                setting.setCoreSize(setting.getMaxSize());
            }
            executor.setCorePoolSize(setting.getCoreSize());
            executor.setMaxPoolSize(setting.getMaxSize());
            executor.setQueueCapacity(setting.getQueueSize());
            executor.setThreadNamePrefix(setting.getPrefix());
            executor.initialize();

            ExecutorService executorService;
            if (this.applicationContext != null) {
                LazyTraceThreadPoolTaskExecutor taskExecutor = new LazyTraceThreadPoolTaskExecutor(
                        this.applicationContext.getAutowireCapableBeanFactory(),
                        executor
                );
                executorService = taskExecutor.getThreadPoolExecutor();
                if (this.registry != null) {
                    Option<ExecutorService> executorServiceOption =
                            this.registry.measureExecutorService(Option.fromNullable(executorService), key);
                    return executorServiceOption.orNull();
                }
            } else {
                executorService = executor.getThreadPoolExecutor();
                if (this.registry != null) {
                    Option<ExecutorService> executorServiceOption =
                            this.registry.measureExecutorService(Option.fromNullable(executorService), key);
                    return executorServiceOption.orNull();
                }
            }
            return executorService;
        }
        log.debug("booster-starter - no thread pool setup for [{}]", key);
        return null;
    }

    /**
     * Retrieves all the keys for created thread pools
     * @return {@link Set} of keys for all created thread pools.
     */
    @NotNull
    @Override
    public Set<String> getKeys() {
        return this.cache.getKeys();
    }

    /**
     * Retrieves an {@link ExecutorService} instance
     * @param key key of the thread pool
     * @return an {@link ExecutorService} instance or null if one cannot be created.
     */
    @Nullable
    @Override
    public ExecutorService get(String key) {
        if (key != null) {
            return this.cache.get(key);
        }
        return null;
    }

    /**
     * Try get a thread pool
     * @param key key for the thread pool. The same key will return the same thread pool instance
     *            if exists.
     * @return {@link Option} of {@link ExecutorService}
     */
    @NotNull
    @Override
    public Option<ExecutorService> tryGet(String key) {
        return this.cache.tryGet(key);
    }
}
