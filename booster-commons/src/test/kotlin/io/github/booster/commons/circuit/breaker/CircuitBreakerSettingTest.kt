package io.github.booster.commons.circuit.breaker

import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.commons.circuit.breaker.CircuitBreakerSetting
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsEqual
import org.hamcrest.core.IsNull
import org.junit.jupiter.api.Test

internal class CircuitBreakerSettingTest {
    @Test
    fun shouldBuildDefault() {
        val setting = CircuitBreakerSetting()
        assertThat(setting, IsNull.notNullValue())
        assertThat(
            setting.failureRateThreshold,
            equalTo(CircuitBreakerSetting.DEFAULT_FAILURE_THRESHOLD)
        )
        assertThat(
            setting.maxWaitDurationInHalfOpenState,
            equalTo(CircuitBreakerSetting.DEFAULT_MAX_WAIT_DURATION_IN_HALF_OPEN_STATE)
        )
        assertThat(
            setting.minimumNumberOfCalls,
            equalTo(CircuitBreakerSetting.DEFAULT_MINIMUM_NUMBER_OF_CALLS)
        )
        assertThat(
            setting.slidingWindowSize,
            equalTo(CircuitBreakerSetting.DEFAULT_SLIDING_WINDOW_SIZE)
        )
        assertThat(
            setting.permittedNumberOfCallsInHalfOpenState,
            equalTo(CircuitBreakerSetting.DEFAULT_PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE)
        )
        assertThat(
            setting.slowCallDurationThreshold,
            equalTo(CircuitBreakerSetting.DEFAULT_SLOW_CALL_DURATION_THRESHOLD)
        )
        assertThat(
            setting.waitDurationInOpenState,
            equalTo(CircuitBreakerSetting.DEFAULT_WAIT_DURATION_IN_OPEN_STATE)
        )
        assertThat(
            setting.slowCallRateThreshold,
            equalTo(CircuitBreakerSetting.DEFAULT_SLOW_CALL_THRESHOLD)
        )
        assertThat(
            setting.slidingWindowType,
            equalTo(CircuitBreakerSetting.SlidingWindowType.COUNT_BASED)
        )
    }

    @Test
    fun shouldBuildWithOutOfRangeValues() {
        val setting = CircuitBreakerSetting()
        assertThat(setting, IsNull.notNullValue())
        setting.failureRateThreshold = 0
        assertThat(
            setting.failureRateThreshold,
            equalTo(CircuitBreakerSetting.DEFAULT_FAILURE_THRESHOLD)
        )
        setting.maxWaitDurationInHalfOpenState = 0
        assertThat(
            setting.maxWaitDurationInHalfOpenState,
            equalTo(CircuitBreakerSetting.DEFAULT_MAX_WAIT_DURATION_IN_HALF_OPEN_STATE)
        )
        setting.minimumNumberOfCalls = 0
        assertThat(
            setting.minimumNumberOfCalls,
            equalTo(CircuitBreakerSetting.DEFAULT_MINIMUM_NUMBER_OF_CALLS)
        )
        setting.slidingWindowSize = 0
        assertThat(
            setting.slidingWindowSize,
            equalTo(CircuitBreakerSetting.DEFAULT_SLIDING_WINDOW_SIZE)
        )
        
        setting.permittedNumberOfCallsInHalfOpenState = 0
        assertThat(
            setting.permittedNumberOfCallsInHalfOpenState,
            equalTo(CircuitBreakerSetting.DEFAULT_PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE)
        )
        
        setting.slowCallDurationThreshold = 0
        assertThat(
            setting.slowCallDurationThreshold,
            equalTo(CircuitBreakerSetting.DEFAULT_SLOW_CALL_DURATION_THRESHOLD)
        )
        
        setting.waitDurationInOpenState = 0
        assertThat(
            setting.waitDurationInOpenState,
            equalTo(CircuitBreakerSetting.DEFAULT_WAIT_DURATION_IN_OPEN_STATE)
        )
        
        setting.slowCallRateThreshold = 0
        assertThat(
            setting.slowCallRateThreshold,
            equalTo(CircuitBreakerSetting.DEFAULT_SLOW_CALL_THRESHOLD)
        )
        
        setting.slidingWindowType = null
        assertThat(
            setting.slidingWindowType,
            equalTo(CircuitBreakerSetting.SlidingWindowType.COUNT_BASED)
        )
    }

    @Test
    fun shouldBuildWithValidValues() {
        val setting = CircuitBreakerSetting()
        assertThat(setting, IsNull.notNullValue())
        setting.failureRateThreshold = 10
        assertThat(
            setting.failureRateThreshold,
            equalTo(10)
        )
        setting.maxWaitDurationInHalfOpenState = 20
        assertThat(
            setting.maxWaitDurationInHalfOpenState,
            equalTo(20)
        )
        setting.minimumNumberOfCalls = 20
        assertThat(
            setting.minimumNumberOfCalls,
            equalTo(20)
        )
        setting.slidingWindowSize = 100
        assertThat(
            setting.slidingWindowSize,
            equalTo(100)
        )
        setting.permittedNumberOfCallsInHalfOpenState = 20
        assertThat(
            setting.permittedNumberOfCallsInHalfOpenState,
            equalTo(20)
        )
        setting.slowCallDurationThreshold = 10
        assertThat(
            setting.slowCallDurationThreshold,
            equalTo(10)
        )
        setting.waitDurationInOpenState = 10
        assertThat(
            setting.waitDurationInOpenState,
            equalTo(10)
        )
        setting.slowCallRateThreshold = 10
        assertThat(
            setting.slowCallRateThreshold,
            equalTo(10)
        )
        setting.slidingWindowType = CircuitBreakerSetting.SlidingWindowType.TIME_BASED
        assertThat(
            setting.slidingWindowType,
            equalTo(CircuitBreakerSetting.SlidingWindowType.TIME_BASED)
        )
    }

    @Test
    fun shouldGetCorrectValue() {
        val setting = CircuitBreakerSetting()
        setting.failureRateThreshold = -1
        assertThat(
            setting.failureRateThreshold,
            equalTo(CircuitBreakerSetting.DEFAULT_FAILURE_THRESHOLD)
        )
        setting.failureRateThreshold = 200
        assertThat(
            setting.failureRateThreshold,
            equalTo(CircuitBreakerSetting.DEFAULT_FAILURE_THRESHOLD)
        )
        setting.slowCallRateThreshold = -1
        assertThat(
            setting.slowCallRateThreshold,
            equalTo(CircuitBreakerSetting.DEFAULT_SLOW_CALL_THRESHOLD)
        )
        setting.slowCallRateThreshold = 200
        assertThat(
            setting.slowCallRateThreshold,
            equalTo(CircuitBreakerSetting.DEFAULT_SLOW_CALL_THRESHOLD)
        )
        setting.isAutomaticTransitionFromOpenToHalfOpenEnabled = true
        assertThat(
            setting.isAutomaticTransitionFromOpenToHalfOpenEnabled,
            equalTo(true)
        )
    }

    @Test
    fun shouldBuild() {
        val setting = CircuitBreakerSetting()
        assertThat(setting.buildCircuitBreaker("test"), IsNull.notNullValue())
        assertThat(setting.buildCircuitBreaker("test", MetricsRegistry(null)), IsNull.notNullValue())
        assertThat(
            setting.buildCircuitBreaker("test", MetricsRegistry(SimpleMeterRegistry())),
            IsNull.notNullValue()
        )
        setting.isAutomaticTransitionFromOpenToHalfOpenEnabled = true
        setting.slidingWindowType = CircuitBreakerSetting.SlidingWindowType.COUNT_BASED
        assertThat(setting.buildCircuitBreaker("test"), IsNull.notNullValue())
        assertThat(setting.buildCircuitBreaker("test", null), IsNull.notNullValue())
    }
}
