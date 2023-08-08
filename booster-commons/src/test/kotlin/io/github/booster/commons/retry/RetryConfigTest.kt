package io.github.booster.commons.retry

import io.github.booster.commons.metrics.MetricsRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.hamcrest.MatcherAssert
import org.hamcrest.core.IsEqual
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.Test

internal class RetryConfigTest {
    @Test
    fun shouldCreateConfig() {
        MatcherAssert.assertThat(RetryConfig(), IsNull.notNullValue())
        MatcherAssert.assertThat(RetryConfig(mapOf()), IsNull.notNullValue())
        MatcherAssert.assertThat(RetryConfig(mapOf(Pair("test", RetrySetting()))), IsNull.notNullValue())
    }

    @Test
    fun shouldNotCreateRetry() {
        MatcherAssert.assertThat(RetryConfig().tryGet("test").isDefined(), IsEqual.equalTo(false))
        MatcherAssert.assertThat(RetryConfig(mapOf()).tryGet("test").isDefined(), IsEqual.equalTo(false))
        MatcherAssert.assertThat(
            RetryConfig(mapOf(Pair("abc", RetrySetting()))).tryGet("test").isDefined(),
            IsEqual.equalTo(false)
        )
    }

    @Test
    fun shouldCreateRetry() {
        val setting = RetrySetting()
        setting.maxAttempts = 1
        MatcherAssert.assertThat(
            RetryConfig(mapOf(Pair("test", setting))).tryGet("test").isDefined(),
            IsEqual.equalTo(true)
        )
    }

    @Test
    fun shouldHandleRegistry() {
        val setting = RetrySetting()
        setting.maxAttempts = 1
        val config = RetryConfig(mapOf(Pair("test", setting)))
        config.setMetricsRegistry(null)
        MatcherAssert.assertThat(
            config.tryGet("test").isDefined(), IsEqual.equalTo(true)
        )
        MatcherAssert.assertThat(
            config.tryGet("abc").isDefined(), IsEqual.equalTo(false)
        )
        config.setMetricsRegistry(MetricsRegistry())
        MatcherAssert.assertThat(
            config.tryGet("test").isDefined(), IsEqual.equalTo(true)
        )
        MatcherAssert.assertThat(
            config.tryGet("abc").isDefined(), IsEqual.equalTo(false)
        )
        config.setMetricsRegistry(MetricsRegistry(SimpleMeterRegistry()))
        MatcherAssert.assertThat(
            config.tryGet("test").isDefined(), IsEqual.equalTo(true)
        )
        MatcherAssert.assertThat(
            config.tryGet("abc").isDefined(), IsEqual.equalTo(false)
        )
    }
}
