package io.github.booster.commons.circuit.breaker

import arrow.core.Option
import arrow.core.Option.Companion.fromNullable
import arrow.core.getOrElse
import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import lombok.ToString
import java.time.Duration

/**
 * Per circuit breaker setting.
 */


const val ONE_HUNDRED_PERCENT = 100

@ToString
class CircuitBreakerSetting {
    /**
     * Sliding window type used for circuit breaking.
     */
    enum class SlidingWindowType {
        /**
         * Count based window
         */
        COUNT_BASED,

        /**
         * Time based window.
         */
        TIME_BASED
    }

    var failureRateThreshold = 0 // 50
        /**
         * Configures the failure rate threshold in percentage.
         *
         * When the failure rate is equal or greater than the threshold the
         * CircuitBreaker transitions to open and starts short-circuiting calls.
         *
         * @return failure rate threshold
         */
        get() = if (field <= 0 || field > ONE_HUNDRED_PERCENT) DEFAULT_FAILURE_THRESHOLD else field
        set(failureRateThreshold) {
            field =
                if (failureRateThreshold <= 0 || failureRateThreshold > ONE_HUNDRED_PERCENT) DEFAULT_FAILURE_THRESHOLD
                else failureRateThreshold
        }
    var slowCallRateThreshold = 0 // 100
        /**
         * Configures a threshold in percentage. The CircuitBreaker considers
         * a call as slow when the call duration is greater than slowCallDurationThreshold
         *
         * When the percentage of slow calls is equal or greater the threshold,
         * the CircuitBreaker transitions to open and starts short-circuiting calls.
         *
         * @return slow call rate threshold
         */
        get() = if (field <= 0 || field > ONE_HUNDRED_PERCENT) DEFAULT_SLOW_CALL_THRESHOLD else field
        set(slowCallRateThreshold) {
            field =
                if (slowCallRateThreshold <= 0 || slowCallRateThreshold > ONE_HUNDRED_PERCENT)
                    DEFAULT_SLOW_CALL_THRESHOLD
                else
                    slowCallRateThreshold
        }
    var slowCallDurationThreshold = 0 // 60000[ms]
        /**
         * Configures the duration threshold above which calls are
         * considered as slow and increase the rate of slow calls.
         *
         * @return slow call duration threshold
         */
        get() = if (field <= 0) DEFAULT_SLOW_CALL_DURATION_THRESHOLD else field
        set(slowCallDurationThreshold) {
            field =
                if (slowCallDurationThreshold <= 0) DEFAULT_SLOW_CALL_DURATION_THRESHOLD else slowCallDurationThreshold
        }
    var permittedNumberOfCallsInHalfOpenState = 0 // 10
        /**
         * Configures the number of permitted calls when the CircuitBreaker is half open.
         *
         * @return  permitted number of calls in half open state
         */
        get() = if (field <= 0) DEFAULT_PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE else field
        set(permittedNumberOfCallsInHalfOpenState) {
            field =
                if (permittedNumberOfCallsInHalfOpenState <= 0) DEFAULT_PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE
                else permittedNumberOfCallsInHalfOpenState
        }
    var maxWaitDurationInHalfOpenState = 0 // 0[ms]
        /**
         * Configures a maximum wait duration which controls the longest amount of
         * time a CircuitBreaker could stay in Half Open state, before it switches to open.
         * Value 0 means Circuit Breaker would wait infinitely in HalfOpen State
         * until all permitted calls have been completed.
         *
         * @return maximum wait duration in half open state
         */
        get() = if (field <= 0) DEFAULT_MAX_WAIT_DURATION_IN_HALF_OPEN_STATE else field
        set(maxWaitDurationInHalfOpenState) {
            field =
                if (maxWaitDurationInHalfOpenState <= 0) DEFAULT_MAX_WAIT_DURATION_IN_HALF_OPEN_STATE
                else maxWaitDurationInHalfOpenState
        }

    var slidingWindowType: SlidingWindowType? = SlidingWindowType.COUNT_BASED
        get() = if (field == null) {
            SlidingWindowType.COUNT_BASED
        } else {
            field
        }
        set(slidingWindowType) {
            field = slidingWindowType ?: SlidingWindowType.COUNT_BASED
        }

    var slidingWindowSize = 0 // 100
        /**
         * Configures the size of the sliding window which is used to
         * record the outcome of calls when the CircuitBreaker is closed.
         *
         * @return sliding window size.
         */
        get() = if (field <= 0) DEFAULT_SLIDING_WINDOW_SIZE else field
        set(slidingWindowSize) {
            field = if (slidingWindowSize <= 0) DEFAULT_SLIDING_WINDOW_SIZE else slidingWindowSize
        }
    var minimumNumberOfCalls = 0 // 100
        /**
         * Configures the minimum number of calls which are required (per sliding window period)
         * before the CircuitBreaker can calculate the error rate or slow call rate.
         * For example, if minimumNumberOfCalls is 10, then at least 10 calls must be recorded,
         * before the failure rate can be calculated.
         * If only 9 calls have been recorded the CircuitBreaker will
         * not transition to open even if all 9 calls have failed.
         *
         * @return  minimum number of calls required to calculate error rate or slow rate.
         */
        get() = if (field <= 0) DEFAULT_MINIMUM_NUMBER_OF_CALLS else field
        set(minimumNumberOfCalls) {
            field = if (minimumNumberOfCalls <= 0) DEFAULT_MINIMUM_NUMBER_OF_CALLS
            else minimumNumberOfCalls
        }
    var waitDurationInOpenState = 0 // 60000[ms]
        /**
         * The time that the CircuitBreaker should wait before transitioning from open to half-open.
         *
         * @return wait duration in open state.
         */
        get() = if (field <= 0) DEFAULT_WAIT_DURATION_IN_OPEN_STATE else field
        set(waitDurationInOpenState) {
            field = if (waitDurationInOpenState <= 0) DEFAULT_WAIT_DURATION_IN_OPEN_STATE
            else waitDurationInOpenState
        }

    /**
     * If set to true it means that the CircuitBreaker will automatically transition
     * from open to half-open state and no call is needed to trigger the transition.
     * A thread is created to monitor all the instances of CircuitBreakers to transition
     * them to HALF_OPEN once waitDurationInOpenState passes. Whereas, if set to false the
     * transition to HALF_OPEN only happens if a call is made, even after waitDurationInOpenState is passed.
     * The advantage here is no thread monitors the state of all CircuitBreakers.
     *
     * @return  automatically transition from open to half open state.
     */
    var isAutomaticTransitionFromOpenToHalfOpenEnabled = false // false

    /**
     * Builds a resilience4j circuit breaker without reporting metrics.
     * @param key name of the circuit breaker.
     * @return an optional [CircuitBreaker]
     */
    @JvmOverloads
    fun buildCircuitBreaker(
        key: String,
        metricsRegistry: MetricsRegistry? = MetricsRegistry(null)
    ): Option<CircuitBreaker> {
        Preconditions.checkArgument(key.isNotEmpty(), "name cannot be null")

        val builder = CircuitBreakerConfig.custom()
            .failureRateThreshold(failureRateThreshold.toFloat())
            .slowCallRateThreshold(slowCallRateThreshold.toFloat())
            .slowCallDurationThreshold(Duration.ofMillis(slowCallDurationThreshold.toLong()))
            .permittedNumberOfCallsInHalfOpenState(permittedNumberOfCallsInHalfOpenState)
            .maxWaitDurationInHalfOpenState(Duration.ofMillis(maxWaitDurationInHalfOpenState.toLong()))
            .slidingWindowType(
                if (slidingWindowType == SlidingWindowType.COUNT_BASED)
                    CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
                else CircuitBreakerConfig.SlidingWindowType.TIME_BASED
            ).slidingWindowSize(slidingWindowSize)
            .minimumNumberOfCalls(minimumNumberOfCalls)
            .waitDurationInOpenState(Duration.ofMillis(waitDurationInOpenState.toLong()))

        if (isAutomaticTransitionFromOpenToHalfOpenEnabled) {
            builder.enableAutomaticTransitionFromOpenToHalfOpen()
        }

        val config = builder.build()
        val circuitBreakerRegistry = CircuitBreakerRegistry.of(config)
        if (metricsRegistry != null && metricsRegistry.registryOption.isDefined()) {
            TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry)
                .bindTo(metricsRegistry.registryOption.getOrElse { SimpleMeterRegistry() })
        }
        return fromNullable<CircuitBreaker>(circuitBreakerRegistry.circuitBreaker(key, config))
    }

    companion object {
        /**
         * Default failure threshold used when not specified.
         */
        const val DEFAULT_FAILURE_THRESHOLD = 50

        /**
         * Default slow call threshold used when not specified.
         */
        const val DEFAULT_SLOW_CALL_THRESHOLD = 100

        /**
         * Default slow call duration threshold used when not specified.
         */
        const val DEFAULT_SLOW_CALL_DURATION_THRESHOLD = 60000

        /**
         * Default permitted number of calls in half open state used when not specified.
         */
        const val DEFAULT_PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE = 10

        /**
         * Default maximum wait duration in half open state used when not specified.
         */
        const val DEFAULT_MAX_WAIT_DURATION_IN_HALF_OPEN_STATE = 1

        /**
         * Default sliding window size used when not specified.
         */
        const val DEFAULT_SLIDING_WINDOW_SIZE = 100

        /**
         * Default minimum number of calls used when not specified.
         */
        const val DEFAULT_MINIMUM_NUMBER_OF_CALLS = 100

        /**
         * Default wait duration in open state used when not specified.
         */
        const val DEFAULT_WAIT_DURATION_IN_OPEN_STATE = 60000
    }
}
