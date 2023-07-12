package io.github.booster.commons.retry;

import io.github.booster.commons.metrics.MetricsRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;

class RetrySettingTest {

    @Test
    void shouldBuildDefault() {
        RetrySetting setting = RetrySetting.builder().build();
        assertThat(setting, notNullValue());
        assertThat(setting.getMaxAttempts(), equalTo(0));
        assertThat(setting.getInitialBackOffMillis(), equalTo(100));
        assertThat(setting.getBackOffPolicy(), equalTo(RetrySetting.BackOffPolicy.LINEAR));
        assertThat(
                setting.buildRetry("test", new MetricsRegistry(new SimpleMeterRegistry())),
                notNullValue()
        );
        assertThat(
                setting.buildRetry("test", new MetricsRegistry(new SimpleMeterRegistry())).isDefined(),
                equalTo(false)
        );
    }

    @Test
    void shouldBuildWithoutRegistry() {
        RetrySetting setting = RetrySetting.builder().build();
        setting.setMaxAttempts(1);
        assertThat(setting, notNullValue());
        assertThat(setting.getMaxAttempts(), equalTo(1));
        assertThat(setting.getInitialBackOffMillis(), equalTo(100));
        assertThat(setting.getBackOffPolicy(), equalTo(RetrySetting.BackOffPolicy.LINEAR));
        assertThat(
                setting.buildRetry("test", null),
                notNullValue()
        );
        assertThat(
                setting.buildRetry("test", null).isDefined(),
                equalTo(true)
        );
        assertThat(
                setting.buildRetry("test", new MetricsRegistry()),
                notNullValue()
        );
        assertThat(
                setting.buildRetry("test", new MetricsRegistry()).isDefined(),
                equalTo(true)
        );

        setting.setBackOffPolicy(RetrySetting.BackOffPolicy.EXPONENTIAL);
        assertThat(
                setting.buildRetry("test", null),
                notNullValue()
        );
        assertThat(
                setting.buildRetry("test", null).isDefined(),
                equalTo(true)
        );
        assertThat(
                setting.buildRetry("test", new MetricsRegistry()),
                notNullValue()
        );
        assertThat(
                setting.buildRetry("test", new MetricsRegistry()).isDefined(),
                equalTo(true)
        );
    }

    @Test
    void shouldBuildWithWrongValues() {
        RetrySetting setting = RetrySetting.builder()
                .maxAttempts(-1)
                .initialBackOffMillis(-1)
                .backOffPolicy(null)
                .build();
        assertThat(setting, notNullValue());
        assertThat(setting.getMaxAttempts(), equalTo(0));
        assertThat(setting.getInitialBackOffMillis(), equalTo(RetrySetting.DEFAULT_INITIAL_BACKOFF_MILLIS));
        assertThat(setting.getBackOffPolicy(), equalTo(RetrySetting.BackOffPolicy.LINEAR));
        assertThat(
                setting.buildRetry("test", new MetricsRegistry(new SimpleMeterRegistry())),
                notNullValue()
        );
        assertThat(
                setting.buildRetry("test", new MetricsRegistry(new SimpleMeterRegistry())).isDefined(),
                equalTo(false)
        );
    }

    @Test
    void shouldBuildAttempts() {
        RetrySetting setting = RetrySetting.builder()
                .maxAttempts(2)
                .backOffPolicy(RetrySetting.BackOffPolicy.EXPONENTIAL)
                .initialBackOffMillis(20)
                .build();
        assertThat(setting, notNullValue());
        assertThat(setting.getMaxAttempts(), equalTo(2));
        assertThat(setting.getInitialBackOffMillis(), equalTo(20));
        assertThat(setting.getBackOffPolicy(), equalTo(RetrySetting.BackOffPolicy.EXPONENTIAL));
        assertThat(
                setting.buildRetry("test", new MetricsRegistry(new SimpleMeterRegistry())),
                notNullValue()
        );
        assertThat(setting.buildRetry("test").isDefined(), equalTo(true));
    }

    @Test
    void shouldTolerateSet() {
        RetrySetting setting = RetrySetting.builder().build();
        setting.setBackOffPolicy(null);
        assertThat(setting.getBackOffPolicy(), equalTo(RetrySetting.BackOffPolicy.LINEAR));
        setting.setBackOffPolicy(RetrySetting.BackOffPolicy.EXPONENTIAL);
        assertThat(setting.getBackOffPolicy(), equalTo(RetrySetting.BackOffPolicy.EXPONENTIAL));

        setting.setMaxAttempts(-1);
        assertThat(setting.getMaxAttempts(), equalTo(0));

        setting.setMaxAttempts(10);
        assertThat(setting.getMaxAttempts(), equalTo(10));

        setting.setInitialBackOffMillis(-1);
        assertThat(setting.getInitialBackOffMillis(), equalTo(RetrySetting.DEFAULT_INITIAL_BACKOFF_MILLIS));

        setting.setInitialBackOffMillis(100);
        assertThat(setting.getInitialBackOffMillis(), equalTo(100));
    }
}
