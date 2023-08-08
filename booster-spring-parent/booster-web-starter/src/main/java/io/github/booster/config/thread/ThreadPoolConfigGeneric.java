package io.github.booster.config.thread;

import arrow.core.Option;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.pool.GenericKeyedObjectPool;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.cloud.sleuth.instrument.async.LazyTraceThreadPoolTaskExecutor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Spring Configuration for thread groups.
 */
public class ThreadPoolConfigGeneric extends GenericKeyedObjectPool<ExecutorService> implements ApplicationContextAware {

    private static final Logger log = LoggerFactory.getLogger(ThreadPoolConfigGeneric.class);

    private Map<String, ThreadPoolSetting> settings = new HashMap<>();

    private MetricsRegistry registry;

    private ApplicationContext applicationContext;

    /**
     * Creates default thread pools upon start up.
     * @param settings thread pool settings.
     */
    public void setSettings(Map<String, ThreadPoolSetting> settings) {
        this.settings = settings == null ? Collections.emptyMap() : settings;
    }

    /**
     * Retrieves original setting.
     * @param name name of the setting.
     * @return {@link ThreadPoolSetting} if exists, otherwise null.
     */
    public ThreadPoolSetting getSetting(String name) {
        if (name != null) {
            return this.settings.get(name);
        }
        return null;
    }

    @PreDestroy
    public void destroy() {
        this.getKeys()
                .forEach(key -> this.get(key).shutdown());
    }

    public void setMetricsRegistry(MetricsRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected ExecutorService createObject(String name) {
        if (this.settings.containsKey(name)) {
            ThreadPoolSetting setting = this.settings.get(name);
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            log.debug("booster-starter - creating thread pool for [{}], setting: [{}]", name, setting);

            if (StringUtils.isBlank(setting.getPrefix())) {
                setting.setPrefix(name);
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
                            this.registry.measureExecutorService(Option.fromNullable(executorService), name);
                    return executorServiceOption.orNull();
                }
            } else {
                executorService = executor.getThreadPoolExecutor();
                if (this.registry != null) {
                    Option<ExecutorService> executorServiceOption =
                            this.registry.measureExecutorService(Option.fromNullable(executorService), name);
                    return executorServiceOption.orNull();
                }
            }
            return executorService;
        }
        log.debug("booster-starter - no thread pool setup for [{}]", name);
        return null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
