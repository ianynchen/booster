package io.github.booster.config

import io.github.booster.commons.circuit.breaker.CircuitBreakerConfig
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.commons.retry.RetryConfig
import io.github.booster.config.BoosterConfig
import io.github.booster.config.example.BoosterSampleApplication
import io.github.booster.config.thread.ThreadPoolConfig
import io.github.booster.factories.TaskFactory
import io.github.booster.http.client.config.HttpClientConnectionConfig
import io.micrometer.core.instrument.MeterRegistry
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [BoosterSampleApplication::class, BoosterConfig::class])
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
    }
}
