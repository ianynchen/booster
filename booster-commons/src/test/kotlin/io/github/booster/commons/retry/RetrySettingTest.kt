package io.github.booster.commons.retry

import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.commons.retry.RetrySetting
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.Test

internal class RetrySettingTest {
    @Test
    fun shouldBuildDefault() {
        val setting = RetrySetting()
        assertThat(setting, notNullValue())
        assertThat(setting.maxAttempts, equalTo(0))
        assertThat(setting.initialBackOffMillis, equalTo(100))
        assertThat(setting.backOffPolicy, equalTo(RetrySetting.BackOffPolicy.LINEAR))
        assertThat(
            setting.buildRetry("test", MetricsRegistry(SimpleMeterRegistry())),
            notNullValue()
        )
        assertThat(
            setting.buildRetry("test", MetricsRegistry(SimpleMeterRegistry())).isDefined(),
            equalTo(false)
        )
    }

    @Test
    fun shouldBuildWithoutRegistry() {
        val setting = RetrySetting()
        setting.maxAttempts = 1
        assertThat(setting, notNullValue())
        assertThat(setting.maxAttempts, equalTo(1))
        assertThat(setting.initialBackOffMillis, equalTo(100))
        assertThat(setting.backOffPolicy, equalTo(RetrySetting.BackOffPolicy.LINEAR))
        assertThat(
            setting.buildRetry("test", null),
            notNullValue()
        )
        assertThat(
            setting.buildRetry("test", null).isDefined(),
            equalTo(true)
        )
        assertThat(
            setting.buildRetry("test", MetricsRegistry()),
            notNullValue()
        )
        assertThat(
            setting.buildRetry("test", MetricsRegistry()).isDefined(),
            equalTo(true)
        )
        setting.backOffPolicy = RetrySetting.BackOffPolicy.EXPONENTIAL
        assertThat(
            setting.buildRetry("test", null),
            notNullValue()
        )
        assertThat(
            setting.buildRetry("test", null).isDefined(),
            equalTo(true)
        )
        assertThat(
            setting.buildRetry("test", MetricsRegistry()),
            notNullValue()
        )
        assertThat(
            setting.buildRetry("test", MetricsRegistry()).isDefined(),
            equalTo(true)
        )
    }

    @Test
    fun shouldBuildWithWrongValues() {
        val setting = RetrySetting()
        setting.maxAttempts =-1
        setting.initialBackOffMillis =-1
        setting.backOffPolicy =null

        assertThat(setting, notNullValue())
        assertThat(setting.maxAttempts, equalTo(0))
        assertThat(
            setting.initialBackOffMillis,
            equalTo(RetrySetting.DEFAULT_INITIAL_BACKOFF_MILLIS)
        )
        assertThat(setting.backOffPolicy, equalTo(RetrySetting.BackOffPolicy.LINEAR))
        assertThat(
            setting.buildRetry("test", MetricsRegistry(SimpleMeterRegistry())),
            notNullValue()
        )
        assertThat(
            setting.buildRetry("test", MetricsRegistry(SimpleMeterRegistry())).isDefined(),
            equalTo(false)
        )
    }

    @Test
    fun shouldBuildAttempts() {
        val setting = RetrySetting()
        setting.maxAttempts = 2
        setting.backOffPolicy = RetrySetting.BackOffPolicy.EXPONENTIAL
        setting.initialBackOffMillis = 20

        assertThat(setting, notNullValue())
        assertThat(setting.maxAttempts, equalTo(2))
        assertThat(setting.initialBackOffMillis, equalTo(20))
        assertThat(setting.backOffPolicy, equalTo(RetrySetting.BackOffPolicy.EXPONENTIAL))
        assertThat(
            setting.buildRetry("test", MetricsRegistry(SimpleMeterRegistry())),
            notNullValue()
        )
        assertThat(setting.buildRetry("test").isDefined(), equalTo(true))
    }

    @Test
    fun shouldTolerateSet() {
        val setting = RetrySetting()
        setting.backOffPolicy = null
        assertThat(setting.backOffPolicy, equalTo(RetrySetting.BackOffPolicy.LINEAR))
        setting.backOffPolicy = RetrySetting.BackOffPolicy.EXPONENTIAL
        assertThat(setting.backOffPolicy, equalTo(RetrySetting.BackOffPolicy.EXPONENTIAL))
        setting.maxAttempts = -1
        assertThat(setting.maxAttempts, equalTo(0))
        setting.maxAttempts = 10
        assertThat(setting.maxAttempts, equalTo(10))
        setting.initialBackOffMillis = -1
        assertThat(
            setting.initialBackOffMillis,
            equalTo(RetrySetting.DEFAULT_INITIAL_BACKOFF_MILLIS)
        )
        setting.initialBackOffMillis = 100
        assertThat(setting.initialBackOffMillis, IsEqual.equalTo(100))
    }
}
