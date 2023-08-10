package io.github.booster.commons.retry

import arrow.core.Option
import arrow.core.Option.Companion.fromNullable
import arrow.core.getOrElse
import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import lombok.ToString
import java.time.Duration

/**
 * Retry config to create retries.
 */
@ToString
class RetrySetting {
    /**
     * retry backoff policy, either using linear backoff time or exponentially increasing backoff time.
     */
    enum class BackOffPolicy {
        /**
         * Linear backoff time.
         */
        LINEAR,

        /**
         * Exponential backoff time.
         */
        EXPONENTIAL
    }

    var backOffPolicy: BackOffPolicy? = null
        /**
         * Backoff policy, either linear or exponential.
         * @return backoff policy, either linear or exponential.
         */
        get() = if (field == null) BackOffPolicy.LINEAR else field
        set(backOffPolicy) {
            field = backOffPolicy ?: BackOffPolicy.LINEAR
        }
    var maxAttempts = 0
        /**
         * Total number of client calls, including the initial one.
         *
         * @return maximum attempts.
         */
        get() = if (field <= 0) 0 else field
        set(maxAttempts) {
            field = if (maxAttempts < 0) 0 else maxAttempts
        }
    var initialBackOffMillis = 0
        /**
         * Initial backoff time in milliseconds.
         * @return initial backoff milliseconds.
         */
        get() = if (field < MINIMUM_INITIAL_BACKOFF_MILLIS) DEFAULT_INITIAL_BACKOFF_MILLIS else field
        set(initialBackOffMillis) {
            field =
                if (initialBackOffMillis < MINIMUM_INITIAL_BACKOFF_MILLIS) DEFAULT_INITIAL_BACKOFF_MILLIS
                else initialBackOffMillis
        }

    /**
     * Builds a resilience4j Retry using name, will also record metrics.
     * @param name name of [Retry]
     * @param metricsRegistry [MetricsRegistry] to record metrics.
     * @return optional [Retry]
     */
    @JvmOverloads
    fun buildRetry(name: String, metricsRegistry: MetricsRegistry? = null): Option<Retry> {
        Preconditions.checkArgument(name.isNotEmpty(), "name cannot be null")
        if (maxAttempts == 0) {
            return fromNullable<Retry>(null)
        }
        val retryConfig = RetryConfig.custom<Any>()
            .maxAttempts(maxAttempts)
            .intervalFunction(
                if (backOffPolicy == BackOffPolicy.LINEAR) IntervalFunction.of(
                    Duration.ofMillis(
                        initialBackOffMillis.toLong()
                    )
                ) else IntervalFunction.ofExponentialBackoff(
                    Duration.ofMillis(
                        initialBackOffMillis.toLong()
                    )
                )
            )
            .build()
        val retryRegistry = RetryRegistry.of(retryConfig)
        if (metricsRegistry != null && metricsRegistry.registryOption.isDefined()) {
            TaggedRetryMetrics.ofRetryRegistry(retryRegistry)
                .bindTo(metricsRegistry.registryOption.getOrElse { SimpleMeterRegistry() })
        }
        return fromNullable(retryRegistry.retry(name, retryConfig))
    }

    companion object {
        /**
         * Default initial backoff time in milliseconds. This
         * value is used if no valid initial backoff time is
         * provided.
         */
        const val DEFAULT_INITIAL_BACKOFF_MILLIS = 100

        /**
         * Minimum initial backoff time in milliseconds.
         */
        const val MINIMUM_INITIAL_BACKOFF_MILLIS = 1
    }
}
