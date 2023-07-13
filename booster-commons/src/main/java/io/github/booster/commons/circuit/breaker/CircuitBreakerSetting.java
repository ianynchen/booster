package io.github.booster.commons.circuit.breaker;

import arrow.core.Option;
import arrow.core.OptionKt;
import com.google.common.base.Preconditions;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.micrometer.tagged.TaggedCircuitBreakerMetrics;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;

/**
 * Per circuit breaker setting.
 */
@ToString
public class CircuitBreakerSetting {

    /**
     * Default failure threshold used when not specified.
     */
    public static final int DEFAULT_FAILURE_THRESHOLD = 50;
    /**
     * Default slow call threshold used when not specified.
     */
    public static final int DEFAULT_SLOW_CALL_THRESHOLD = 100;
    /**
     * Default slow call duration threshold used when not specified.
     */
    public static final int DEFAULT_SLOW_CALL_DURATION_THRESHOLD = 60000;
    /**
     * Default permitted number of calls in half open state used when not specified.
     */
    public static final int DEFAULT_PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE = 10;
    /**
     * Default maximum wait duration in half open state used when not specified.
     */
    public static final int DEFAULT_MAX_WAIT_DURATION_IN_HALF_OPEN_STATE = 1;
    /**
     * Default sliding window size used when not specified.
     */
    public static final int DEFAULT_SLIDING_WINDOW_SIZE = 100;
    /**
     * Default minimum number of calls used when not specified.
     */
    public static final int DEFAULT_MINIMUM_NUMBER_OF_CALLS = 100;
    /**
     * Default wait duration in open state used when not specified.
     */
    public static final int DEFAULT_WAIT_DURATION_IN_OPEN_STATE = 60000;

    /**
     * Sliding window type used for circuit breaking.
     */
    public enum SlidingWindowType {
        /**
         * Count based window
         */
        COUNT_BASED,
        /**
         * Time based window.
         */
        TIME_BASED,
    }

    private int failureRateThreshold; // 50

    private int slowCallRateThreshold; // 100

    private int slowCallDurationThreshold; // 60000[ms]

    private int permittedNumberOfCallsInHalfOpenState; // 10

    private int maxWaitDurationInHalfOpenState; // 0[ms]

    private SlidingWindowType slidingWindowType; // COUNT_BASED

    private int slidingWindowSize; // 100

    private int minimumNumberOfCalls; // 100

    private int waitDurationInOpenState; // 60000[ms]

    private boolean automaticTransitionFromOpenToHalfOpenEnabled; // false

    /**
     * Configures the failure rate threshold in percentage.
     *
     * When the failure rate is equal or greater than the threshold the
     * CircuitBreaker transitions to open and starts short-circuiting calls.
     *
     * @return failure rate threshold
     */
    public int getFailureRateThreshold() {
        return failureRateThreshold <= 0 || failureRateThreshold > 100 ?
                DEFAULT_FAILURE_THRESHOLD : failureRateThreshold;
    }

    public void setFailureRateThreshold(int failureRateThreshold) {
        this.failureRateThreshold =
                (failureRateThreshold <= 0 || failureRateThreshold > 100) ?
                        DEFAULT_FAILURE_THRESHOLD : failureRateThreshold;
    }

    /**
     * Configures a threshold in percentage. The CircuitBreaker considers
     * a call as slow when the call duration is greater than slowCallDurationThreshold
     *
     * When the percentage of slow calls is equal or greater the threshold,
     * the CircuitBreaker transitions to open and starts short-circuiting calls.
     *
     * @return slow call rate threshold
     */
    public int getSlowCallRateThreshold() {
        return slowCallRateThreshold <= 0 || slowCallRateThreshold > 100 ?
                DEFAULT_SLOW_CALL_THRESHOLD : slowCallRateThreshold;
    }

    public void setSlowCallRateThreshold(int slowCallRateThreshold) {
        this.slowCallRateThreshold =
                (slowCallRateThreshold <= 0 || slowCallRateThreshold > 100) ?
                        DEFAULT_SLOW_CALL_THRESHOLD : slowCallRateThreshold;
    }

    /**
     * Configures the duration threshold above which calls are
     * considered as slow and increase the rate of slow calls.
     *
     * @return slow call duration threshold
     */
    public int getSlowCallDurationThreshold() {
        return slowCallDurationThreshold <= 0 ?
                DEFAULT_SLOW_CALL_DURATION_THRESHOLD : slowCallDurationThreshold;
    }

    public void setSlowCallDurationThreshold(int slowCallDurationThreshold) {
        this.slowCallDurationThreshold =
                slowCallDurationThreshold <= 0 ?
                        DEFAULT_SLOW_CALL_DURATION_THRESHOLD : slowCallDurationThreshold;
    }

    /**
     * Configures the number of permitted calls when the CircuitBreaker is half open.
     *
     * @return  permitted number of calls in half open state
     */
    public int getPermittedNumberOfCallsInHalfOpenState() {
        return permittedNumberOfCallsInHalfOpenState <= 0 ?
                DEFAULT_PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE : permittedNumberOfCallsInHalfOpenState;
    }

    public void setPermittedNumberOfCallsInHalfOpenState(int permittedNumberOfCallsInHalfOpenState) {
        this.permittedNumberOfCallsInHalfOpenState =
                permittedNumberOfCallsInHalfOpenState <= 0 ?
                        DEFAULT_PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE : permittedNumberOfCallsInHalfOpenState;
    }

    /**
     * Configures a maximum wait duration which controls the longest amount of
     * time a CircuitBreaker could stay in Half Open state, before it switches to open.
     * Value 0 means Circuit Breaker would wait infinitely in HalfOpen State
     * until all permitted calls have been completed.
     *
     * @return maximum wait duration in half open state
     */
    public int getMaxWaitDurationInHalfOpenState() {
        return maxWaitDurationInHalfOpenState <= 0 ?
                DEFAULT_MAX_WAIT_DURATION_IN_HALF_OPEN_STATE : maxWaitDurationInHalfOpenState;
    }

    public void setMaxWaitDurationInHalfOpenState(int maxWaitDurationInHalfOpenState) {
        this.maxWaitDurationInHalfOpenState =
                maxWaitDurationInHalfOpenState <= 0 ?
                        DEFAULT_MAX_WAIT_DURATION_IN_HALF_OPEN_STATE : maxWaitDurationInHalfOpenState;
    }

    /**
     * Configures the type of the sliding window which is used to record the
     * outcome of calls when the CircuitBreaker is closed.
     * Sliding window can either be count-based or time-based.
     *
     * If the sliding window is COUNT_BASED, the last slidingWindowSize calls are recorded and aggregated.
     * If the sliding window is TIME_BASED, the calls of the last slidingWindowSize seconds recorded and aggregated.
     *
     * @return  sliding window type, either count based or time based.
     */
    public SlidingWindowType getSlidingWindowType() {
        return slidingWindowType == null ? SlidingWindowType.COUNT_BASED : slidingWindowType;
    }

    public void setSlidingWindowType(SlidingWindowType slidingWindowType) {
        this.slidingWindowType = slidingWindowType == null ?
                SlidingWindowType.COUNT_BASED : slidingWindowType;
    }

    /**
     * Configures the size of the sliding window which is used to
     * record the outcome of calls when the CircuitBreaker is closed.
     *
     * @return sliding window size.
     */
    public int getSlidingWindowSize() {
        return slidingWindowSize <= 0 ? DEFAULT_SLIDING_WINDOW_SIZE : slidingWindowSize;
    }

    public void setSlidingWindowSize(int slidingWindowSize) {
        this.slidingWindowSize = slidingWindowSize <= 0 ?
                DEFAULT_SLIDING_WINDOW_SIZE : slidingWindowSize;
    }

    /**
     * Configures the minimum number of calls which are required (per sliding window period)
     * before the CircuitBreaker can calculate the error rate or slow call rate.
     * For example, if minimumNumberOfCalls is 10, then at least 10 calls must be recorded, before the failure rate can be calculated.
     * If only 9 calls have been recorded the CircuitBreaker will not transition to open even if all 9 calls have failed.
     *
     * @return  minimum number of calls required to calculate error rate or slow rate.
     */
    public int getMinimumNumberOfCalls() {
        return minimumNumberOfCalls <= 0 ? DEFAULT_MINIMUM_NUMBER_OF_CALLS : minimumNumberOfCalls;
    }

    public void setMinimumNumberOfCalls(int minimumNumberOfCalls) {
        this.minimumNumberOfCalls = minimumNumberOfCalls <= 0 ?
                DEFAULT_MINIMUM_NUMBER_OF_CALLS : minimumNumberOfCalls;
    }

    /**
     * The time that the CircuitBreaker should wait before transitioning from open to half-open.
     *
     * @return wait duration in open state.
     */
    public int getWaitDurationInOpenState() {
        return waitDurationInOpenState <= 0 ?
                DEFAULT_WAIT_DURATION_IN_OPEN_STATE : waitDurationInOpenState;
    }

    public void setWaitDurationInOpenState(int waitDurationInOpenState) {
        this.waitDurationInOpenState = waitDurationInOpenState <= 0 ?
                DEFAULT_WAIT_DURATION_IN_OPEN_STATE : waitDurationInOpenState;
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
    public boolean isAutomaticTransitionFromOpenToHalfOpenEnabled() {
        return automaticTransitionFromOpenToHalfOpenEnabled;
    }

    public void setAutomaticTransitionFromOpenToHalfOpenEnabled(
            boolean automaticTransitionFromOpenToHalfOpenEnabled
    ) {
        this.automaticTransitionFromOpenToHalfOpenEnabled = automaticTransitionFromOpenToHalfOpenEnabled;
    }

    /**
     * Builds a resilience4j circuit breaker without reporting metrics.
     * @param name name of the circuit breaker.
     * @return an optional {@link CircuitBreaker}
     */
    public Option<CircuitBreaker> buildCircuitBreaker(String name) {
        return this.buildCircuitBreaker(name, new MetricsRegistry(null));
    }

    /**
     * Builds a resilience4j circuit breaker with metrics reported.
     * @param name name of the circuit breaker.
     * @param metricsRegistry {@link MetricsRegistry}
     * @return optional circuit breaker.
     */
    public Option<CircuitBreaker> buildCircuitBreaker(String name, MetricsRegistry metricsRegistry) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "name cannot be null");
        CircuitBreakerConfig.Builder builder = CircuitBreakerConfig.custom()
                .failureRateThreshold(this.getFailureRateThreshold())
                .slowCallRateThreshold(this.getSlowCallRateThreshold())
                .slowCallDurationThreshold(Duration.ofMillis(this.getSlowCallDurationThreshold()))
                .permittedNumberOfCallsInHalfOpenState(this.getPermittedNumberOfCallsInHalfOpenState())
                .maxWaitDurationInHalfOpenState(Duration.ofMillis(this.getMaxWaitDurationInHalfOpenState()))
                .slidingWindowType(this.slidingWindowType == SlidingWindowType.COUNT_BASED ?
                        CircuitBreakerConfig.SlidingWindowType.COUNT_BASED :
                        CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
                .slidingWindowSize(this.getSlidingWindowSize())
                .minimumNumberOfCalls(this.getMinimumNumberOfCalls())
                .waitDurationInOpenState(Duration.ofMillis(this.getWaitDurationInOpenState()));

        if (this.isAutomaticTransitionFromOpenToHalfOpenEnabled()) {
            builder.enableAutomaticTransitionFromOpenToHalfOpen();
        }

        CircuitBreakerConfig config = builder.build();
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(config);

        if (metricsRegistry != null && metricsRegistry.getRegistry().isDefined()) {
            TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry)
                    .bindTo(OptionKt.getOrElse(metricsRegistry.getRegistry(), () -> null));
        }

        return Option.fromNullable(circuitBreakerRegistry.circuitBreaker(name, config));
    }
}
