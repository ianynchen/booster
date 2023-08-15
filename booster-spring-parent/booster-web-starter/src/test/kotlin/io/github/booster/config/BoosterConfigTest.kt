package io.github.booster.config

import io.github.booster.commons.circuit.breaker.CircuitBreakerConfig
import io.github.booster.commons.circuit.breaker.CircuitBreakerSetting
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.commons.retry.RetryConfig
import io.github.booster.commons.retry.RetrySetting
import io.github.booster.config.BoosterConfig
import io.github.booster.config.example.BoosterSampleApplication
import io.github.booster.config.thread.ThreadPoolConfig
import io.github.booster.factories.TaskFactory
import io.github.booster.http.client.config.HttpClientConnectionConfig
import io.micrometer.core.instrument.MeterRegistry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [BoosterSampleApplication::class, BoosterConfig::class])
@EnableConfigurationProperties
internal class BoosterConfigTest {
    @Autowired
    private var config: BoosterConfig? = null

    @Autowired
    private var registry: MetricsRegistry? = null

    @Autowired
    private var threadPoolConfig: ThreadPoolConfig? = null

    @Autowired
    private var retryConfig: RetryConfig? = null

    @Autowired
    private var circuitBreakerConfig: CircuitBreakerConfig? = null

    @Autowired
    private var taskFactory: TaskFactory? = null

    @Autowired
    private var httpClientConnectionConfig: HttpClientConnectionConfig? = null

    @Autowired
    private var meterRegistryCustomizer: MeterRegistryCustomizer<MeterRegistry>? = null
    @Test
    fun shouldCreate() {
        assertThat(config, notNullValue())
        assertThat(registry, notNullValue())
        assertThat(threadPoolConfig, notNullValue())
        assertThat(retryConfig, notNullValue())
        assertThat(circuitBreakerConfig, notNullValue())
        assertThat(taskFactory, notNullValue())
        assertThat(httpClientConnectionConfig, notNullValue())
        assertThat(meterRegistryCustomizer, notNullValue())

        this.verifyHttpClientConnectionConfig(this.httpClientConnectionConfig!!)
        this.verifyRetryConfig(this.retryConfig!!)
        this.verifyCircuitBreakerConfig(this.circuitBreakerConfig!!)
        this.verifyThreadPoolConfig(this.threadPoolConfig!!)
    }

    private fun verifyThreadPoolConfig(config: ThreadPoolConfig) {
        assertThat(
            config.getSettings()["test"]?.coreSize, equalTo(50)
        )
        assertThat(
            config.getSettings()["test"]?.maxSize, equalTo(200)
        )
        assertThat(
            config.getSettings()["test"]?.queueSize, equalTo(1000)
        )
    }

    private fun verifyCircuitBreakerConfig(config: CircuitBreakerConfig) {
        assertThat(
            config.getSettings()["test"]?.isAutomaticTransitionFromOpenToHalfOpenEnabled,
            equalTo(true)
        )
        assertThat(config.getSettings()["test"]?.failureRateThreshold, equalTo(40))
        assertThat(config.getSettings()["test"]?.slowCallRateThreshold, equalTo(15))
        assertThat(config.getSettings()["test"]?.slowCallDurationThreshold, equalTo(20000))
        assertThat(config.getSettings()["test"]?.permittedNumberOfCallsInHalfOpenState, equalTo(15))
        assertThat(config.getSettings()["test"]?.maxWaitDurationInHalfOpenState, equalTo(15))
        assertThat(
            config.getSettings()["test"]?.slidingWindowType,
            equalTo(CircuitBreakerSetting.SlidingWindowType.TIME_BASED)
        )
        assertThat(config.getSettings()["test"]?.slidingWindowSize, equalTo(20))
        assertThat(config.getSettings()["test"]?.minimumNumberOfCalls, equalTo(10))
        assertThat(config.getSettings()["test"]?.waitDurationInOpenState, equalTo(10000))
    }

    private fun verifyRetryConfig(config: RetryConfig) {
        assertThat(config.getSettings()["test"]?.maxAttempts, equalTo(5))
        assertThat(config.getSettings()["test"]?.initialBackOffMillis, equalTo(20))
        assertThat(config.getSettings()["test"]?.backOffPolicy, equalTo(RetrySetting.BackOffPolicy.EXPONENTIAL))
    }

    private fun verifyHttpClientConnectionConfig(config: HttpClientConnectionConfig) {
        assertThat(config.settings["test"]?.baseUrl, equalTo("http://test.com"))
        assertThat(config.settings["test"]?.connectionTimeoutMillis, equalTo(1000))
        assertThat(config.settings["test"]?.isFollowRedirects, equalTo(true))
        assertThat(config.settings["test"]?.isUseSSL, equalTo(true))
        assertThat(config.settings["test"]?.maxInMemorySizeMB, equalTo(1000))
        assertThat(config.settings["test"]?.readTimeoutMillis, equalTo(1000))
        assertThat(config.settings["test"]?.writeTimeoutMillis, equalTo(1000))
        assertThat(config.settings["test"]?.responseTimeoutInMillis, equalTo(1000))
        assertThat(config.settings["test"]?.sslHandshakeTimeoutMillis, equalTo(1000))
    }
}
