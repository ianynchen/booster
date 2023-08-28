package io.github.booster.commons.circuit.breaker

import io.github.booster.commons.metrics.MetricsRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

internal class CircuitBreakerConfigTest {
    @Test
    fun shouldCreateConfig() {
        assertThat(CircuitBreakerConfig(), notNullValue())
        assertThat(CircuitBreakerConfig(mapOf()), notNullValue())
        assertThat(CircuitBreakerConfig(mapOf(Pair("test", CircuitBreakerSetting()))), notNullValue())
    }

    @Test
    fun shouldNotCreateCircuitBreaker() {
        assertThat(CircuitBreakerConfig().tryGet("test").isDefined(), equalTo(false))
        assertThat(CircuitBreakerConfig(mapOf()).tryGet("test").isDefined(), equalTo(false))
        assertThat(
            CircuitBreakerConfig(mapOf(Pair("abc", CircuitBreakerSetting()))).tryGet("test").isDefined(),
            equalTo(false)
        )
    }

    @Test
    fun shouldCreateCircuitBreaker() {
        assertThat(
            CircuitBreakerConfig(mapOf(Pair("test", CircuitBreakerSetting()))).tryGet("test").isDefined(),
            equalTo(true)
        )
    }

    @Test
    fun shouldHandleRegistry() {
        val config = CircuitBreakerConfig(mapOf(Pair("test", CircuitBreakerSetting())))
        config.setMetricsRegistry(null)
        assertThat(
            config.tryGet("test").isDefined(), equalTo(true)
        )
        assertThat(
            config.tryGet("abc").isDefined(), equalTo(false)
        )
        config.setMetricsRegistry(MetricsRegistry())
        assertThat(
            config.tryGet("test").isDefined(), equalTo(true)
        )
        assertThat(
            config.tryGet("abc").isDefined(), equalTo(false)
        )
        config.setMetricsRegistry(MetricsRegistry(SimpleMeterRegistry()))
        assertThat(
            config.tryGet("test").isDefined(), equalTo(true)
        )
        assertThat(
            config.tryGet("abc").isDefined(), equalTo(false)
        )
    }
}
