package io.github.booster.config.thread;

import io.github.booster.commons.metrics.MetricsRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

class ThreadPoolConfigTest {

    private ThreadPoolSetting setting;

    @BeforeEach
    void setup() {
        this.setting = new ThreadPoolSetting();
    }

    @Test
    void shouldCreatePool() {
        ThreadPoolConfigGeneric config = new ThreadPoolConfigGeneric();
        config.setMetricsRegistry(new MetricsRegistry(new SimpleMeterRegistry()));
        config.setSettings(Map.of("test", setting));

        assertThat(config.get("abc"), nullValue());
        ExecutorService service = config.get("test");
        assertThat(service, notNullValue());
        ExecutorService anotherReference = config.get("test");
        assertThat(anotherReference, sameInstance(service));
    }

    @Test
    void shouldNotBreak() {
        ThreadPoolConfigGeneric config = new ThreadPoolConfigGeneric();
        config.setSettings(null);
        assertThat(config.get("abc"), nullValue());

        assertThat(config.getSetting("abc"), nullValue());
    }

    @Test
    void shouldCreateThreadPool() {
        ThreadPoolSetting setting = new ThreadPoolSetting();
        setting.setCoreSize(12);
        setting.setMaxSize(2);
        ThreadPoolConfigGeneric config = new ThreadPoolConfigGeneric();
        config.setSettings(Map.of("test", setting));
        assertThat(config.get("test"), notNullValue());

        assertThat(config.getSetting("test"), notNullValue());
    }

    @Test
    void shouldCreatePoolWithNoRegistry() {
        ThreadPoolConfigGeneric config = new ThreadPoolConfigGeneric();
        config.setMetricsRegistry(null);
        config.setSettings(Map.of("test", setting));

        assertThat(config.get("abc"), nullValue());
        ExecutorService service = config.get("test");
        assertThat(service, notNullValue());
        ExecutorService anotherReference = config.get("test");
        assertThat(anotherReference, sameInstance(service));
    }
}
