package io.github.booster.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.booster.commons.circuit.breaker.CircuitBreakerConfig
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.commons.retry.RetryConfig
import io.github.booster.config.thread.ThreadPoolConfig
import io.github.booster.factories.HttpClientFactory
import io.github.booster.factories.TaskFactory
import io.github.booster.http.client.config.CustomWebClientExchangeTagsProvider
import io.github.booster.http.client.config.HttpClientConnectionConfig
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.BeansException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.actuate.metrics.web.reactive.client.WebClientExchangeTagsProvider
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

/**
 * Auto configuration for Booster Web, HTTP client and tasks.
 */
@Configuration
class BoosterConfig : ApplicationContextAware {
    private var applicationContext: ApplicationContext? = null

    /**
     * Creates metrics recorder
     *
     * @param meterRegistry internal [MeterRegistry] to be used to record metrics
     * @param recordTrace   whether to include trace ID in metrics as a tag.
     *                      If set to true, will add traceId and value of the trace in tags.
     * @return [MetricsRegistry]
     */
    @Bean
    fun metricsRegistry(
        @Autowired(required = false) meterRegistry: MeterRegistry?,
        @Value("\${booster.metrics.recordTrace:false}") recordTrace: Boolean
    ): MetricsRegistry {
        log.debug("booster-starter - record with trace: [{}]", recordTrace)
        return MetricsRegistry(meterRegistry, recordTrace)
    }

    @Bean
    @ConfigurationProperties(prefix = "booster.task.threads")
    fun threadPoolConfig(
        @Autowired registry: MetricsRegistry
    ): ThreadPoolConfig {
        val threadPoolConfig = ThreadPoolConfig()
        threadPoolConfig.setMetricsRegistry(registry)
        return threadPoolConfig
    }

    @Bean
    @ConfigurationProperties(prefix = "booster.task.circuit-breaker")
    fun circuitBreakerConfig(): CircuitBreakerConfig {
        return CircuitBreakerConfig()
    }

    @Bean
    @ConfigurationProperties(prefix = "booster.http.client.connection")
    fun httpClientConnectionConfig(): HttpClientConnectionConfig {
        return HttpClientConnectionConfig(applicationContext)
    }

    @Bean
    fun clientRequestObservationConvention(): WebClientExchangeTagsProvider {
        return CustomWebClientExchangeTagsProvider()
    }

    @Bean
    @ConfigurationProperties(prefix = "booster.task.retry")
    fun retryConfig(): RetryConfig {
        return RetryConfig()
    }

    @Bean
    fun httpClientFactory(
        @Autowired config: HttpClientConnectionConfig,
        @Autowired webClientBuilder: WebClient.Builder,
        @Autowired(required = false) mapper: ObjectMapper?
    ): HttpClientFactory {
        return HttpClientFactory(
            config,
            webClientBuilder,
            mapper ?: jacksonObjectMapper()
        )
    }

    @Bean
    fun taskFactory(
        @Autowired threadPoolConfig: ThreadPoolConfig,
        @Autowired retryConfig: RetryConfig,
        @Autowired circuitBreakerConfig: CircuitBreakerConfig,
        @Autowired httpClientFactory: HttpClientFactory,
        @Autowired registry: MetricsRegistry
    ): TaskFactory {
        return TaskFactory(
            threadPoolConfig,
            retryConfig,
            circuitBreakerConfig,
            httpClientFactory,
            registry
        )
    }

    @Bean
    fun metricsCommonTags(
        @Value("\${spring.application.name:default}") serviceName: String,
        @Value("\${spring.profiles.active:default}") activeProfile: String
    ): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry: MeterRegistry ->
            registry.config().commonTags(
                NAME, serviceName,
                PROFILE, activeProfile
            )
        }
    }

    @Throws(BeansException::class)
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }

    companion object {
        const val NAME = "service"
        const val PROFILE = "environment"
        private val log = LoggerFactory.getLogger(BoosterConfig::class.java)
    }
}
