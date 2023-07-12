package io.github.booster.commons.circuit.breaker;

import io.github.booster.commons.metrics.MetricsRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

class CircuitBreakerSettingTest {

    @Test
    void shouldBuildDefault() {
        CircuitBreakerSetting setting = new CircuitBreakerSetting();
        assertThat(setting, notNullValue());
        assertThat(
                setting.getFailureRateThreshold(),
                equalTo(CircuitBreakerSetting.DEFAULT_FAILURE_THRESHOLD)
        );
        assertThat(
                setting.getMaxWaitDurationInHalfOpenState(),
                equalTo(CircuitBreakerSetting.DEFAULT_MAX_WAIT_DURATION_IN_HALF_OPEN_STATE)
        );
        assertThat(
                setting.getMinimumNumberOfCalls(),
                equalTo(CircuitBreakerSetting.DEFAULT_MINIMUM_NUMBER_OF_CALLS)
        );
        assertThat(
                setting.getSlidingWindowSize(),
                equalTo(CircuitBreakerSetting.DEFAULT_SLIDING_WINDOW_SIZE)
        );
        assertThat(
                setting.getPermittedNumberOfCallsInHalfOpenState(),
                equalTo(CircuitBreakerSetting.DEFAULT_PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE)
        );
        assertThat(
                setting.getSlowCallDurationThreshold(),
                equalTo(CircuitBreakerSetting.DEFAULT_SLOW_CALL_DURATION_THRESHOLD)
        );
        assertThat(
                setting.getWaitDurationInOpenState(),
                equalTo(CircuitBreakerSetting.DEFAULT_WAIT_DURATION_IN_OPEN_STATE)
        );
        assertThat(
                setting.getSlowCallRateThreshold(),
                equalTo(CircuitBreakerSetting.DEFAULT_SLOW_CALL_THRESHOLD)
        );
        assertThat(
                setting.getSlidingWindowType(),
                equalTo(CircuitBreakerSetting.SlidingWindowType.COUNT_BASED)
        );
    }

    @Test
    void shouldBuildWithOutOfRangeValues() {
        CircuitBreakerSetting setting = new CircuitBreakerSetting();
        assertThat(setting, notNullValue());
        setting.setFailureRateThreshold(0);
        assertThat(
                setting.getFailureRateThreshold(),
                equalTo(CircuitBreakerSetting.DEFAULT_FAILURE_THRESHOLD)
        );
        setting.setMaxWaitDurationInHalfOpenState(0);
        assertThat(
                setting.getMaxWaitDurationInHalfOpenState(),
                equalTo(CircuitBreakerSetting.DEFAULT_MAX_WAIT_DURATION_IN_HALF_OPEN_STATE)
        );
        setting.setMinimumNumberOfCalls(0);
        assertThat(
                setting.getMinimumNumberOfCalls(),
                equalTo(CircuitBreakerSetting.DEFAULT_MINIMUM_NUMBER_OF_CALLS)
        );
        setting.setSlidingWindowSize(0);
        assertThat(
                setting.getSlidingWindowSize(),
                equalTo(CircuitBreakerSetting.DEFAULT_SLIDING_WINDOW_SIZE)
        );
        setting.setPermittedNumberOfCallsInHalfOpenState(0);
        assertThat(
                setting.getPermittedNumberOfCallsInHalfOpenState(),
                equalTo(CircuitBreakerSetting.DEFAULT_PERMITTED_NUMBER_OF_CALLS_IN_HALF_OPEN_STATE)
        );
        setting.setSlowCallDurationThreshold(0);
        assertThat(
                setting.getSlowCallDurationThreshold(),
                equalTo(CircuitBreakerSetting.DEFAULT_SLOW_CALL_DURATION_THRESHOLD)
        );
        setting.setWaitDurationInOpenState(0);
        assertThat(
                setting.getWaitDurationInOpenState(),
                equalTo(CircuitBreakerSetting.DEFAULT_WAIT_DURATION_IN_OPEN_STATE)
        );
        setting.setSlowCallRateThreshold(0);
        assertThat(
                setting.getSlowCallRateThreshold(),
                equalTo(CircuitBreakerSetting.DEFAULT_SLOW_CALL_THRESHOLD)
        );
        setting.setSlidingWindowType(null);
        assertThat(
                setting.getSlidingWindowType(),
                equalTo(CircuitBreakerSetting.SlidingWindowType.COUNT_BASED)
        );
    }

    @Test
    void shouldBuildWithValidValues() {
        CircuitBreakerSetting setting = new CircuitBreakerSetting();
        assertThat(setting, notNullValue());
        setting.setFailureRateThreshold(10);
        assertThat(
                setting.getFailureRateThreshold(),
                equalTo(10)
        );
        setting.setMaxWaitDurationInHalfOpenState(20);
        assertThat(
                setting.getMaxWaitDurationInHalfOpenState(),
                equalTo(20)
        );
        setting.setMinimumNumberOfCalls(20);
        assertThat(
                setting.getMinimumNumberOfCalls(),
                equalTo(20)
        );
        setting.setSlidingWindowSize(100);
        assertThat(
                setting.getSlidingWindowSize(),
                equalTo(100)
        );
        setting.setPermittedNumberOfCallsInHalfOpenState(20);
        assertThat(
                setting.getPermittedNumberOfCallsInHalfOpenState(),
                equalTo(20)
        );
        setting.setSlowCallDurationThreshold(10);
        assertThat(
                setting.getSlowCallDurationThreshold(),
                equalTo(10)
        );
        setting.setWaitDurationInOpenState(10);
        assertThat(
                setting.getWaitDurationInOpenState(),
                equalTo(10)
        );
        setting.setSlowCallRateThreshold(10);
        assertThat(
                setting.getSlowCallRateThreshold(),
                equalTo(10)
        );
        setting.setSlidingWindowType(CircuitBreakerSetting.SlidingWindowType.TIME_BASED);
        assertThat(
                setting.getSlidingWindowType(),
                equalTo(CircuitBreakerSetting.SlidingWindowType.TIME_BASED)
        );
    }

    @Test
    void shouldGetCorrectValue() {
        CircuitBreakerSetting setting = new CircuitBreakerSetting();
        setting.setFailureRateThreshold(-1);
        assertThat(setting.getFailureRateThreshold(), equalTo(CircuitBreakerSetting.DEFAULT_FAILURE_THRESHOLD));
        setting.setFailureRateThreshold(200);
        assertThat(setting.getFailureRateThreshold(), equalTo(CircuitBreakerSetting.DEFAULT_FAILURE_THRESHOLD));

        setting.setSlowCallRateThreshold(-1);
        assertThat(setting.getSlowCallRateThreshold(), equalTo(CircuitBreakerSetting.DEFAULT_SLOW_CALL_THRESHOLD));
        setting.setSlowCallRateThreshold(200);
        assertThat(setting.getSlowCallRateThreshold(), equalTo(CircuitBreakerSetting.DEFAULT_SLOW_CALL_THRESHOLD));

        setting.setAutomaticTransitionFromOpenToHalfOpenEnabled(true);
        assertThat(
                setting.isAutomaticTransitionFromOpenToHalfOpenEnabled(),
                equalTo(true)
        );
    }

    @Test
    void shouldBuild() {
        CircuitBreakerSetting setting = new CircuitBreakerSetting();
        assertThat(setting.buildCircuitBreaker("test"), notNullValue());
        assertThat(setting.buildCircuitBreaker("test", new MetricsRegistry(null)), notNullValue());
        assertThat(setting.buildCircuitBreaker("test", new MetricsRegistry(new SimpleMeterRegistry())), notNullValue());

        setting.setAutomaticTransitionFromOpenToHalfOpenEnabled(true);
        setting.setSlidingWindowType(CircuitBreakerSetting.SlidingWindowType.COUNT_BASED);
        assertThat(setting.buildCircuitBreaker("test"), notNullValue());
        assertThat(setting.buildCircuitBreaker("test", null), notNullValue());
    }
}
