package io.github.booster.config.thread

import io.github.booster.commons.metrics.MetricsRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.CoreMatchers.sameInstance
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Map

internal class ThreadPoolConfigTest {
    private var setting: ThreadPoolSetting? = null
    @BeforeEach
    fun setup() {
        setting = ThreadPoolSetting(
            null,
            null,
            null,
            null
        )
    }

    @Test
    fun shouldCreatePool() {
        val config = ThreadPoolConfig()
        config.setMetricsRegistry(MetricsRegistry(SimpleMeterRegistry()))
        config.setSettings(mapOf(Pair("test", this.setting!!)))
        assertThat(config["abc"], nullValue())
        val service = config["test"]
        assertThat(service, notNullValue())
        val anotherReference = config["test"]
        assertThat(anotherReference, sameInstance(service))
    }

    @Test
    fun shouldNotBreak() {
        val config = ThreadPoolConfig()
        config.setSettings(null)
        assertThat(config["abc"], nullValue())
        assertThat(config.getSetting("abc"), nullValue())
    }

    @Test
    fun shouldCreateThreadPool() {
        val setting = ThreadPoolSetting(
            12,
            2,
            null,
            null
        )
        val config = ThreadPoolConfig()
        config.setSettings(mapOf(Pair("test", setting)))
        assertThat(config["test"], notNullValue())
        assertThat(config.getSetting("test"), notNullValue())
    }

    @Test
    fun shouldCreatePoolWithNoRegistry() {
        val config = ThreadPoolConfig()
        config.setMetricsRegistry(null)
        config.setSettings(mapOf(Pair("test", this.setting!!)))
        assertThat(config["abc"], nullValue())
        val service = config["test"]
        assertThat(service, notNullValue())
        val anotherReference = config["test"]
        assertThat(anotherReference, sameInstance(service))
    }
}
