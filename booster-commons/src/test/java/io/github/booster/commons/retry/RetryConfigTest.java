package io.github.booster.commons.retry;

import io.github.booster.commons.metrics.MetricsRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetryConfigTest {


    @Test
    void shouldThrowException() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> new RetryConfig().getOption("test")
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> new RetryConfig(Map.of()).getOption("test")
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> new RetryConfig(Map.of("test", new RetrySetting())).getOption("test")
        );
    }

    @Test
    void shouldCreateConfig() {
        assertThat(new RetryConfig(), notNullValue());
        assertThat(new RetryConfig(Map.of()), notNullValue());
        assertThat(new RetryConfig(Map.of("test", new RetrySetting())), notNullValue());
    }

    @Test
    void shouldNotCreateRetry() {
        assertThat(new RetryConfig().get("test").isDefined(), equalTo(false));
        assertThat(new RetryConfig(Map.of()).get("test").isDefined(), equalTo(false));
        assertThat(
                new RetryConfig(Map.of("abc", new RetrySetting())).get("test").isDefined(),
                equalTo(false)
        );
    }

    @Test
    void shouldCreateRetry() {
        RetrySetting setting = new RetrySetting();
        setting.setMaxAttempts(1);

        assertThat(
                new RetryConfig(Map.of("test", setting)).get("test").isDefined(),
                equalTo(true)
        );
    }

    @Test
    void shouldHandleRegistry() {
        RetrySetting setting = new RetrySetting();
        setting.setMaxAttempts(1);
        RetryConfig config = new RetryConfig(Map.of("test", setting));

        config.setMetricsRegistry(null);
        assertThat(
                config.get("test").isDefined(), equalTo(true)
        );
        assertThat(
                config.get("abc").isDefined(), equalTo(false)
        );

        config.setMetricsRegistry(new MetricsRegistry());
        assertThat(
                config.get("test").isDefined(), equalTo(true)
        );
        assertThat(
                config.get("abc").isDefined(), equalTo(false)
        );

        config.setMetricsRegistry(new MetricsRegistry(new SimpleMeterRegistry()));
        assertThat(
                config.get("test").isDefined(), equalTo(true)
        );
        assertThat(
                config.get("abc").isDefined(), equalTo(false)
        );
    }
}
