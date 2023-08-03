package io.github.booster.commons.metrics;

import arrow.core.Option;
import com.google.common.base.Preconditions;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import io.micrometer.core.instrument.internal.TimedExecutorService;
import io.opentelemetry.api.trace.Span;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.stream.Stream;

/**
 * Micrometer registry wrapper that allows one to
 * insert metric recording code without Micrometer support.
 */
@Getter
public class MetricsRegistry {

    private static final Logger log = LoggerFactory.getLogger(MetricsRegistry.class);

    /**
     * Trace ID tag.
     */
    public static final String TRACE_ID = "traceId";
    private final Option<MeterRegistry> registry;

    private final boolean recordTrace;

    /**
     * Constructs a noop registry. No metrics will be reported.
     */
    public MetricsRegistry() {
        this(null, false);
    }

    /**
     * Constructor with provided {@link MeterRegistry}
     * @param registry {@link MeterRegistry}, if null behaves the same as noop constructor.
     */
    public MetricsRegistry(MeterRegistry registry) {
        this(registry, false);
    }

    /**
     * Constructor with provided {@link MeterRegistry}
     * @param registry {@link MeterRegistry}, if null behaves the same as noop constructor.
     * @param recordTrace whether to include trace ID in metrics reported. this can
     *                    cause a cardinality issue.
     */
    public MetricsRegistry(MeterRegistry registry, boolean recordTrace) {
        this.registry = Option.fromNullable(registry);
        this.recordTrace = recordTrace;
    }

    /**
     * Creates trace tags given a {@link Span}
     * @param span span where the trace can be obtained.
     * @param tags existing tags.
     * @return a new set of tags with trace id inserted as a tag.
     */
    @SafeVarargs
    public static String[] createTraceTags(Span span, String... tags) {
        if (span != null) {
            String[] newTags = new String[tags.length + 2];
            System.arraycopy(tags, 0, newTags, 0, tags.length);
            newTags[tags.length] = TRACE_ID;
            newTags[tags.length + 1] = span.getSpanContext().getTraceId();
            return newTags;
        }
        return tags;
    }

    private String[] insertTraceTag(String... tags) {
        if (recordTrace && Stream.of(tags).noneMatch(tag -> Objects.equals(tag, TRACE_ID))) {
            Span span = Span.current();
            if (span != null && span.getSpanContext().isValid()) {
                return MetricsRegistry.createTraceTags(span, tags);
            }
        }
        return tags;
    }

    /**
     * Start a timer sample.
     * @return Optional sample
     */
    public Option<Timer.Sample> startSample() {
        return this.registry.map(reg -> Timer.start(reg));
    }

    /**
     * Stop a timer sample and record the time.
     * @param sampleTimer sample to stop
     * @param name name of the timer
     * @param tags optional tags for the timer.
     */
    public void endSample(Option<Timer.Sample> sampleTimer, String name, String... tags) {
        registry.map(reg -> sampleTimer.map(sample -> sample.stop(reg.timer(name, this.insertTraceTag(tags)))));
    }

    /**
     * Increase counter by 1
     * @param name name of the counter to increase
     * @param tags tags for the counter
     */
    public void incrementCounter(String name, String... tags) {
        registry.map(reg -> reg.counter(name, this.insertTraceTag(tags))).map(counter -> {
            counter.increment();
            return counter;
        });
    }

    /**
     * Increase counter by specified amount.
     * @param name name of counter to increase
     * @param increment amount to increase
     * @param tags tags for the counter
     */
    public void incrementCounter(String name, double increment, String... tags) {
        registry.map(reg -> reg.counter(name, this.insertTraceTag(tags))).map(counter -> {
            counter.increment(increment);
            return counter;
        });
    }

    /**
     * Set value for gauge.
     * @param state initial state of the gauge value.
     * @param name name of the gauge.
     * @param tags tags for the gauge
     * @param <T> Type of gauge value.
     * @return Optional value of the gauge state.
     */
    public <T extends Number> Option<T> gauge(T state, String name, String... tags) {
        return this.registry.map(reg -> {
            T gaugeState = reg.gauge(name,
                    Tags.of(this.insertTraceTag(tags)),
                    state);
            return gaugeState;
        });
    }

    /**
     * Monitor thread pool usage
     * @param executorService {@link ExecutorService} to be monitored
     * @param name value to use for name tag
     * @return a monitored {@link ExecutorService}
     */
    public Option<ExecutorService> measureExecutorService(Option<ExecutorService> executorService, String name) {

        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank");

        final Option<ExecutorService> executorServiceOption = executorService == null ?
                Option.fromNullable(null) : executorService;

        Option<ExecutorService> monitoredExecutorOption = this.registry.flatMap(reg ->
                executorServiceOption.map(executor -> {
                    log.debug("booster-task - attempting to measure thread pool: [{}]", name);
                    // if already monitored, return directly.
                    if (executor instanceof TimedExecutorService) {
                        log.debug("booster-task - thread pool [{}] already monitored", name);
                        return executor;
                    }
                    // otherwise, monitor it.
                    log.debug("booster-task - monitoring thread pool [{}]", name);
                    return ExecutorServiceMetrics.monitor(reg, executor, name);
                }));
        return monitoredExecutorOption.isEmpty() ? executorServiceOption : monitoredExecutorOption;
    }
}
