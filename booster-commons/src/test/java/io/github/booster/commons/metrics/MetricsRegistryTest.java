package io.github.booster.commons.metrics;

import arrow.core.Option;
import arrow.core.OptionKt;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.trace.Span;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsSame.sameInstance;

class MetricsRegistryTest {

    private MeterRegistry meterRegistry;

    @BeforeEach
    void setup() {
        this.meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    void shouldBuild() {
        assertThat(this.meterRegistry, notNullValue());
        assertThat(new MetricsRegistry(this.meterRegistry), notNullValue());
    }

    @Test
    void shouldCreateTimer() {
        MetricsRegistry registry = new MetricsRegistry(this.meterRegistry, true);
        Option<Timer.Sample> sampleOption = registry.startSample();
        registry.endSample(sampleOption, "test", "tag", "value");
        assertThat(sampleOption.isDefined(), equalTo(true));
    }

    @Test
    void shouldCreateCounter() {
        MetricsRegistry registry = new MetricsRegistry(this.meterRegistry, true);
        registry.incrementCounter("test1");
        registry.incrementCounter("test1", 2);
        registry.incrementCounter("test2", "tag", "value");
        registry.incrementCounter("test2", 4, "tag", "value");
        assertThat(registry, notNullValue());
        assertThat(registry.getRegistry().isDefined(), equalTo(true));
    }

    @Test
    void shouldCreateGauge() {
        MetricsRegistry registry = new MetricsRegistry(this.meterRegistry, true);
        assertThat(registry, notNullValue());
        assertThat(registry.getRegistry().isDefined(), equalTo(true));

        Option<AtomicInteger> gauge = registry.gauge(new AtomicInteger(0), "gauge", "tag", "value");
        assertThat(gauge, notNullValue());
        assertThat(gauge.isDefined(), equalTo(true));
        gauge.map(it -> it.incrementAndGet());
        assertThat(OptionKt.getOrElse(gauge, () -> null).get(), equalTo(1));
    }

    @Test
    void shouldCreateTags() {
        Span span = Span.current();
        assertThat(span, notNullValue());
        List<String> tags = Arrays.asList(MetricsRegistry.createTraceTags(span, "abc", "def"));
        assertThat(tags, hasItems("abc", "def", MetricsRegistry.TRACE_ID));
    }

    @Test
    void shouldCreateNoTracingTag() {
        List<String> tags = Arrays.asList(MetricsRegistry.createTraceTags(null, "abc", "def"));
        assertThat(tags, contains("abc", "def"));
    }

    @Test
    void shouldReturnEmptyExecutor() {
        MetricsRegistry registry = new MetricsRegistry(new SimpleMeterRegistry());

        assertThat(
                registry.measureExecutorService(null, "abc").isDefined(),
                equalTo(false)
        );
        assertThat(
                registry.measureExecutorService(Option.fromNullable(null), "abc").isDefined(),
                equalTo(false)
        );
        assertThat(
                new MetricsRegistry().measureExecutorService(null, "abc").isDefined(),
                equalTo(false)
        );
        assertThat(
                new MetricsRegistry().measureExecutorService(Option.fromNullable(null), "abc").isDefined(),
                equalTo(false)
        );
    }

    @Test
    void shouldReturnExecutor() {
        MetricsRegistry registry = new MetricsRegistry(new SimpleMeterRegistry());

        ExecutorService executorService = Executors.newCachedThreadPool();
        Option<ExecutorService> monitoredExecutorServiceOption =
                registry.measureExecutorService(Option.fromNullable(executorService), "abc");

        assertThat(
                monitoredExecutorServiceOption.isDefined(),
                equalTo(true)
        );
        assertThat(monitoredExecutorServiceOption.toList(), hasSize(1));
        assertThat(monitoredExecutorServiceOption.toList().get(0), not(sameInstance(executorService)));

        Option<ExecutorService> unMonitoredExecutorServiceOption =
                new MetricsRegistry().measureExecutorService(Option.fromNullable(executorService), "abc");
        assertThat(unMonitoredExecutorServiceOption.isDefined(), equalTo(true));
        assertThat(unMonitoredExecutorServiceOption.toList(), hasSize(1));
        assertThat(unMonitoredExecutorServiceOption.toList().get(0), sameInstance(executorService));
    }
}
