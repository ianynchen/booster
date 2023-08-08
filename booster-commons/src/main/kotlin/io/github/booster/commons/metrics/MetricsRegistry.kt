package io.github.booster.commons.metrics

import arrow.core.Option
import arrow.core.Option.Companion.fromNullable
import arrow.core.getOrElse
import arrow.core.orElse
import com.google.common.base.Preconditions
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics
import io.micrometer.core.instrument.internal.TimedExecutorService
import io.opentelemetry.api.trace.Span
import lombok.Getter
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.stream.Stream

/**
 * Micrometer registry wrapper that allows one to
 * insert metric recording code without Micrometer support.
 */
@Getter
class MetricsRegistry @JvmOverloads constructor(
    registry: MeterRegistry? = null,
    private val recordTrace: Boolean = false
) {
    private val registry: Option<MeterRegistry>
    /**
     * Constructor with provided [MeterRegistry]
     * @param registry [MeterRegistry], if null behaves the same as noop constructor.
     * @param recordTrace whether to include trace ID in metrics reported. this can
     * cause a cardinality issue.
     */
    /**
     * Constructs a noop registry. No metrics will be reported.
     */
    /**
     * Constructor with provided [MeterRegistry]
     * @param registry [MeterRegistry], if null behaves the same as noop constructor.
     */
    init {
        this.registry = fromNullable(registry)
    }

    val registryOption: Option<MeterRegistry>
        get() {
            return this.registry
        }

    private fun insertTraceTag(vararg tags: String): Array<String> {
        if (recordTrace && Stream.of(*tags).noneMatch { tag: String -> tag == TRACE_ID }) {
            val span = Span.current()
            if (span != null && span.spanContext.isValid) {
                return createTraceTags(span, *tags)
            }
        }
        return arrayOf(*tags)
    }

    /**
     * Start a timer sample.
     * @return Optional sample
     */
    fun startSample(): Option<Timer.Sample> {
        return registry.map { Timer.start(it) }
    }

    /**
     * Stop a timer sample and record the time.
     * @param sampleTimer sample to stop
     * @param name name of the timer
     * @param tags optional tags for the timer.
     */
    @Suppress("SpreadOperator")
    fun endSample(sampleTimer: Option<Timer.Sample>, name: String, vararg tags: String) {
        registry.map { reg: MeterRegistry ->
            sampleTimer.map { sample: Timer.Sample ->
                sample.stop(
                    reg.timer(
                        name,
                        *insertTraceTag(*tags)
                    )
                )
            }
        }
    }

    /**
     * Increase counter by 1
     * @param name name of the counter to increase
     * @param tags tags for the counter
     */
    @Suppress("SpreadOperator")
    fun incrementCounter(name: String, vararg tags: String) {
        registry.map { reg: MeterRegistry -> reg.counter(name, *insertTraceTag(*tags)) }
            .map { counter: Counter ->
                counter.increment()
                counter
            }
    }

    /**
     * Increase counter by specified amount.
     * @param name name of counter to increase
     * @param increment amount to increase
     * @param tags tags for the counter
     */
    @Suppress("SpreadOperator")
    fun incrementCounter(name: String, increment: Double, vararg tags: String) {
        registry.map { reg: MeterRegistry -> reg.counter(name, *insertTraceTag(*tags)) }
            .map { counter: Counter ->
                counter.increment(increment)
                counter
            }
    }

    /**
     * Set value for gauge.
     * @param state initial state of the gauge value.
     * @param name name of the gauge.
     * @param tags tags for the gauge
     * @param <T> Type of gauge value.
     * @return Optional value of the gauge state.
    </T> */
    @Suppress("SpreadOperator")
    fun <T : Number> gauge(state: T, name: String, vararg tags: String): Option<T> {
        return registry.map {
            fromNullable(
                it.gauge(
                    name,
                    Tags.of(*insertTraceTag(*tags)),
                    state
                )
            )
        }.getOrElse { fromNullable(null) }
    }

    /**
     * Monitor thread pool usage
     * @param executorService [ExecutorService] to be monitored
     * @param name value to use for name tag
     * @return a monitored [ExecutorService]
     */
    fun measureExecutorService(executorService: Option<ExecutorService>, name: String): Option<ExecutorService> {
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank")

        return registry.flatMap { reg: MeterRegistry? ->
            executorService.map { executor: ExecutorService? ->
                log.debug("booster-task - attempting to measure thread pool: [{}]", name)
                // if already monitored, return directly.
                if (executor is TimedExecutorService) {
                    log.debug("booster-task - thread pool [{}] already monitored", name)
                    return@map executor
                }
                // otherwise, monitor it.
                log.debug("booster-task - monitoring thread pool [{}]", name)
                ExecutorServiceMetrics.monitor(reg!!, executor!!, name)
            }
        }.orElse { executorService }
    }

    companion object {
        private val log = LoggerFactory.getLogger(MetricsRegistry::class.java)

        /**
         * Trace ID tag.
         */
        const val TRACE_ID = "traceId"

        /**
         * Creates trace tags given a [Span]
         * @param span span where the trace can be obtained.
         * @param tags existing tags.
         * @return a new set of tags with trace id inserted as a tag.
         */
        @SafeVarargs
        fun createTraceTags(span: Span?, vararg tags: String): Array<String> {
            if (span != null) {
                var newTags = arrayOf(*tags)

                newTags += TRACE_ID
                newTags += span.spanContext.traceId
                return newTags
            }
            return arrayOf(*tags)
        }
    }
}
