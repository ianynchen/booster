package io.github.booster.config;

import io.github.booster.commons.circuit.breaker.CircuitBreakerConfig;
import io.github.booster.commons.circuit.breaker.CircuitBreakerSetting;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.retry.RetryConfig;
import io.github.booster.commons.retry.RetrySetting;
import io.github.booster.config.example.BoosterSampleApplication;
import io.github.booster.config.thread.ThreadPoolConfig;
import io.github.booster.factories.TaskFactory;
import io.github.booster.http.client.config.HttpClientConnectionConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(classes = {BoosterSampleApplication.class, BoosterConfig.class})
class BoosterConfigTest {

    @Autowired
    private BoosterConfig config;

    @Autowired
    private MetricsRegistry registry;

    @Autowired
    private ThreadPoolConfig threadPoolConfig;

    @Autowired
    private RetryConfig retryConfig;

    @Autowired
    private CircuitBreakerConfig circuitBreakerConfig;

    @Autowired
    private TaskFactory taskFactory;

    @Autowired
    private HttpClientConnectionConfig httpClientConnectionConfig;

    @Autowired
    private MeterRegistryCustomizer<MeterRegistry> meterRegistryCustomizer;

    @Test
    void shouldCreate() {
        assertThat(this.config, notNullValue());
        assertThat(this.registry, notNullValue());
        assertThat(this.threadPoolConfig, notNullValue());
        assertThat(this.retryConfig, notNullValue());
        assertThat(this.circuitBreakerConfig, notNullValue());
        assertThat(this.taskFactory, notNullValue());
        assertThat(this.httpClientConnectionConfig, notNullValue());
        assertThat(this.meterRegistryCustomizer, notNullValue());

        verifyCircuitBreakerConfig(this.circuitBreakerConfig);
        verifyRetryConfig(this.retryConfig);
        verifyHttpClientConnectionConfig(this.httpClientConnectionConfig);
        verifyThreadPoolConfig(this.threadPoolConfig);
    }

    private void verifyThreadPoolConfig(ThreadPoolConfig config) {
        assertThat(config.getSetting("test"), notNullValue());
        assertThat(config.getSetting("test").getCoreSize(), equalTo(20));
        assertThat(config.getSetting("test").getMaxSize(), equalTo(50));
        assertThat(config.getSetting("test").getQueueSize(), equalTo(1_000));
        assertThat(config.getSetting("test").getPrefix(), equalTo("test-pl-"));
    }

    private void verifyCircuitBreakerConfig(CircuitBreakerConfig config) {
        assertThat(config, notNullValue());
        assertThat(config.getSettings().get("test").getFailureRateThreshold(), equalTo(60));
        assertThat(config.getSettings().get("test").getSlowCallRateThreshold(), equalTo(70));
        assertThat(config.getSettings().get("test").getSlowCallDurationThreshold(), equalTo(30_000));
        assertThat(config.getSettings().get("test").getPermittedNumberOfCallsInHalfOpenState(), equalTo(15));
        assertThat(config.getSettings().get("test").getMaxWaitDurationInHalfOpenState(), equalTo(10));
        assertThat(config.getSettings().get("test").getSlidingWindowSize(), equalTo(200));
        assertThat(config.getSettings().get("test").getMinimumNumberOfCalls(), equalTo(30));
        assertThat(config.getSettings().get("test").getWaitDurationInOpenState(), equalTo(20_000));
        assertThat(config.getSettings().get("test").isAutomaticTransitionFromOpenToHalfOpenEnabled(), equalTo(true));
        assertThat(config.getSettings().get("test").getSlidingWindowType(), equalTo(CircuitBreakerSetting.SlidingWindowType.TIME_BASED));
    }

    private void verifyRetryConfig(RetryConfig config) {
        assertThat(config, notNullValue());
        assertThat(config.getSettings().get("test").getBackOffPolicy(), equalTo(RetrySetting.BackOffPolicy.EXPONENTIAL));
        assertThat(config.getSettings().get("test").getMaxAttempts(), equalTo(5));
        assertThat(config.getSettings().get("test").getInitialBackOffMillis(), equalTo(1_000));
    }

    private void verifyHttpClientConnectionConfig(HttpClientConnectionConfig config) {
        assertThat(config, notNullValue());
        assertThat(config.getSettings().get("test").getBaseUrl(), equalTo("http://test.com"));
        assertThat(config.getSettings().get("test").getConnectionTimeoutMillis(), equalTo(1_000));
        assertThat(config.getSettings().get("test").getReadTimeoutMillis(), equalTo(1_000));
        assertThat(config.getSettings().get("test").getWriteTimeoutMillis(), equalTo(1_000));
        assertThat(config.getSettings().get("test").isUseSSL(), equalTo(true));
        assertThat(config.getSettings().get("test").isFollowRedirects(), equalTo(true));
        assertThat(config.getSettings().get("test").getSslHandshakeTimeoutMillis(), equalTo(1_000));
        assertThat(config.getSettings().get("test").getMaxInMemorySizeMB(), equalTo(1_000));
        assertThat(config.getSettings().get("test").getResponseTimeoutInMillis(), equalTo(1_000L));
        assertThat(config.getSettings().get("test").getPool().getMaxConnections(), equalTo(200));
        assertThat(config.getSettings().get("test").getPool().getMaxIdleTimeMillis(), equalTo(10_000L));
        assertThat(config.getSettings().get("test").getPool().getMaxLifeTimeMillis(), equalTo(10_000L));
    }
}
