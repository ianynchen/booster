package io.github.booster.http.client.config;

import io.github.booster.http.client.HttpClientConfigTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientCustomizer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpClientConnectionConfigTest {

    private ConfigurableApplicationContext applicationContext;

    private final MetricsWebClientCustomizer customizer = mock(MetricsWebClientCustomizer.class);

    @BeforeEach
    void setup() {
        this.applicationContext = mock(ConfigurableApplicationContext.class);
        when(this.applicationContext.getBean(MetricsWebClientCustomizer.class))
                .thenReturn(customizer);
        doNothing().when(this.customizer).customize(any());
    }

    @Test
    void shouldFail() {
        HttpClientConnectionConfig config = HttpClientConfigTestData.getConfig(this.applicationContext);
        assertThrows(
                IllegalArgumentException.class,
                () -> config.create(WebClient.builder(), "abc")
        );
    }

    @Test
    void shouldCreateSSLClient() {

        HttpClientConnectionConfig config = HttpClientConfigTestData.getConfig(this.applicationContext);
        WebClient client = config.create(WebClient.builder(), "test");
        assertThat(client, notNullValue());
    }

    @Test
    void shouldCreateNonSSLClient() {

        HttpClientConnectionConfig config = HttpClientConfigTestData.getConfig(this.applicationContext);
        WebClient client = config.create(WebClient.builder(), "test");
        assertThat(client, notNullValue());
    }
}
