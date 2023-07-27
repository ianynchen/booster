package io.github.booster.commons.circuit.breaker;

import io.github.booster.commons.metrics.MetricsRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CircuitBreakerConfigTest {

    @Test
    void shouldThrowException() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> new CircuitBreakerConfig().getOption("test")
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> new CircuitBreakerConfig(Map.of()).getOption("test")
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> new CircuitBreakerConfig(Map.of("test", new CircuitBreakerSetting())).getOption("test")
        );
    }

    @Test
    void shouldCreateConfig() {
        assertThat(new CircuitBreakerConfig(), notNullValue());
        assertThat(new CircuitBreakerConfig(Map.of()), notNullValue());
        assertThat(new CircuitBreakerConfig(Map.of("test", new CircuitBreakerSetting())), notNullValue());
    }

    @Test
    void shouldNotCreateCircuitBreaker() {
        assertThat(new CircuitBreakerConfig().getOption("test").isDefined(), equalTo(false));
        assertThat(new CircuitBreakerConfig(Map.of()).getOption("test").isDefined(), equalTo(false));
        assertThat(
                new CircuitBreakerConfig(Map.of("abc", new CircuitBreakerSetting())).getOption("test").isDefined(),
                equalTo(false)
        );
    }

    @Test
    void shouldCreateCircuitBreaker() {
        assertThat(
                new CircuitBreakerConfig(Map.of("test", new CircuitBreakerSetting())).getOption("test").isDefined(),
                equalTo(true)
        );
    }

    @Test
    void shouldHandleRegistry() {
        CircuitBreakerConfig config = new CircuitBreakerConfig(Map.of("test", new CircuitBreakerSetting()));

        config.setMetricsRegistry(null);
        assertThat(
                config.getOption("test").isDefined(), equalTo(true)
        );
        assertThat(
                config.getOption("abc").isDefined(), equalTo(false)
        );

        config.setMetricsRegistry(new MetricsRegistry());
        assertThat(
                config.getOption("test").isDefined(), equalTo(true)
        );
        assertThat(
                config.getOption("abc").isDefined(), equalTo(false)
        );

        config.setMetricsRegistry(new MetricsRegistry(new SimpleMeterRegistry()));
        assertThat(
                config.getOption("test").isDefined(), equalTo(true)
        );
        assertThat(
                config.getOption("abc").isDefined(), equalTo(false)
        );
    }
}
