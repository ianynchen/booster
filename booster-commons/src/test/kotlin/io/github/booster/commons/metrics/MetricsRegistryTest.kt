package io.github.booster.commons.metrics

import arrow.core.Option
import arrow.core.Option.Companion.fromNullable
import arrow.core.getOrElse
import io.github.booster.commons.metrics.MetricsRegistry
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.opentelemetry.api.trace.Span
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItems
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.sameInstance
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.hasSize
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Arrays
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

internal class MetricsRegistryTest {
    private var meterRegistry: MeterRegistry? = null
    @BeforeEach
    fun setup() {
        meterRegistry = SimpleMeterRegistry()
    }

    @Test
    fun shouldBuild() {
        assertThat(meterRegistry, notNullValue())
        assertThat(MetricsRegistry(meterRegistry), notNullValue())
    }

    @Test
    fun shouldCreateTimer() {
        val registry = MetricsRegistry(meterRegistry, true)
        val sampleOption = registry.startSample()
        registry.endSample(sampleOption, "test", "tag", "value")
        assertThat(sampleOption.isDefined(), equalTo(true))
    }

    @Test
    fun shouldCreateCounter() {
        val registry = MetricsRegistry(meterRegistry, true)
        registry.incrementCounter("test1")
        registry.incrementCounter("test1", 2.0)
        registry.incrementCounter("test2", "tag", "value")
        registry.incrementCounter("test2", 4.0, "tag", "value")
        assertThat(registry, notNullValue())
        assertThat(registry.registryOption.isDefined(), equalTo(true))
    }

    @Test
    fun shouldCreateGauge() {
        val registry = MetricsRegistry(meterRegistry, true)
        assertThat(registry, notNullValue())
        assertThat(registry.registryOption.isDefined(), equalTo(true))
        val gauge = registry.gauge(AtomicInteger(0), "gauge", "tag", "value")
        assertThat(gauge, notNullValue())
        assertThat(gauge.isDefined(), equalTo(true))
        gauge.map { it.incrementAndGet() }
        assertThat(gauge.getOrElse { null }!!.get(), equalTo(1))
    }

    @Test
    fun shouldCreateTags() {
        val span = Span.current()
        assertThat(span, IsNull.notNullValue())
        val tags = Arrays.asList(*MetricsRegistry.createTraceTags(span, "abc", "def"))
        assertThat(tags, hasItems("abc", "def", MetricsRegistry.TRACE_ID))
    }

    @Test
    fun shouldCreateNoTracingTag() {
        val tags = Arrays.asList(*MetricsRegistry.createTraceTags(null, "abc", "def"))
        assertThat(tags, contains("abc", "def"))
    }

    @Test
    fun shouldReturnEmptyExecutor() {
        val registry = MetricsRegistry(SimpleMeterRegistry())
        assertThat(
            registry.measureExecutorService(fromNullable<ExecutorService>(null), "abc").isDefined(),
            equalTo(false)
        )
        assertThat(
            MetricsRegistry().measureExecutorService(fromNullable(null), "abc").isDefined(),
            equalTo(false)
        )
        assertThat(
            MetricsRegistry().measureExecutorService(fromNullable<ExecutorService>(null), "abc")
                .isDefined(),
            equalTo(false)
        )
    }

    @Test
    fun shouldReturnExecutor() {
        val registry = MetricsRegistry(SimpleMeterRegistry())
        val executorService = Executors.newCachedThreadPool()
        val monitoredExecutorServiceOption =
            registry.measureExecutorService(fromNullable(executorService), "abc")
        assertThat(
            monitoredExecutorServiceOption.isDefined(),
            equalTo(true)
        )
        assertThat(monitoredExecutorServiceOption.toList(), Matchers.hasSize(1))
        assertThat(
            monitoredExecutorServiceOption.toList()[0],
            not(sameInstance(executorService))
        )
        val unMonitoredExecutorServiceOption =
            MetricsRegistry().measureExecutorService(fromNullable(executorService), "abc")
        assertThat(unMonitoredExecutorServiceOption.isDefined(), equalTo(true))
        assertThat(unMonitoredExecutorServiceOption.toList(), hasSize(1))
        assertThat(unMonitoredExecutorServiceOption.toList()[0], sameInstance(executorService))
    }
}
