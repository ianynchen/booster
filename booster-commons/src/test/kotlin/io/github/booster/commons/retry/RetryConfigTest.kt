package io.github.booster.commons.retry

import io.github.booster.commons.metrics.MetricsRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

internal class RetryConfigTest {
    @Test
    fun shouldCreateConfig() {
        assertThat(RetryConfig(), notNullValue())
        assertThat(RetryConfig(mapOf()), notNullValue())
        assertThat(RetryConfig(mapOf(Pair("test", RetrySetting()))), notNullValue())
    }

    @Test
    fun shouldNotCreateRetry() {
        assertThat(RetryConfig().tryGet("test").isDefined(), equalTo(false))
        assertThat(RetryConfig(mapOf()).tryGet("test").isDefined(), equalTo(false))
        assertThat(
            RetryConfig(mapOf(Pair("abc", RetrySetting()))).tryGet("test").isDefined(),
            equalTo(false)
        )
    }

    @Test
    fun shouldCreateRetry() {
        val setting = RetrySetting()
        setting.maxAttempts = 1
        assertThat(
            RetryConfig(mapOf(Pair("test", setting))).tryGet("test").isDefined(),
            equalTo(true)
        )
    }

    @Test
    fun `should allow null setting`() {
        val config = RetryConfig()
        config.setSettings(null)

        assertThat(
            config.tryGet("name").isDefined(),
            equalTo(false)
        )
    }

    @Test
    fun `should allow non-null setting`() {
        val setting = RetrySetting()
        setting.maxAttempts = 1

        val config = RetryConfig()
        config.setSettings(mapOf(Pair("test", setting)))

        assertThat(
            config.tryGet("test").isDefined(),
            equalTo(true)
        )
    }

    @Test
    fun shouldHandleRegistry() {
        val setting = RetrySetting()
        setting.maxAttempts = 1
        val config = RetryConfig(mapOf(Pair("test", setting)))
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
