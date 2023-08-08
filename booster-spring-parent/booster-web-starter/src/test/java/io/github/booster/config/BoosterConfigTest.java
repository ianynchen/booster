package io.github.booster.config;

import io.github.booster.commons.circuit.breaker.CircuitBreakerConfig;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.retry.RetryConfig;
import io.github.booster.config.example.BoosterSampleApplication;
import io.github.booster.config.thread.ThreadPoolConfigGeneric;
import io.github.booster.factories.TaskFactory;
import io.github.booster.http.client.config.HttpClientConnectionConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(classes = {BoosterSampleApplication.class, BoosterConfig.class})
class BoosterConfigTest {

    @Autowired
    private BoosterConfig config;

    @Autowired
    private MetricsRegistry registry;

    @Autowired
    private ThreadPoolConfigGeneric threadPoolConfig;

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
    }
}
