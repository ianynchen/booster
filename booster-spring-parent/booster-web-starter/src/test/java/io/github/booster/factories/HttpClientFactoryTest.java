package io.github.booster.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.booster.config.BoosterConfig;
import io.github.booster.config.example.BoosterSampleApplication;
import io.github.booster.http.client.config.HttpClientConnectionConfig;
import io.github.booster.http.client.config.HttpClientConnectionSetting;
import io.github.booster.http.client.impl.HttpClientImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;


@SpringBootTest(classes = {BoosterSampleApplication.class, BoosterConfig.class})
class HttpClientFactoryTest {

    @Autowired
    private HttpClientConnectionConfig config;

    @Test
    void shouldCreate() {
        HttpClientConnectionSetting setting = new HttpClientConnectionSetting();
        setting.setBaseUrl("http://www.ibm.com");
        config.setSettings(Map.of("test", setting));

        HttpClientFactory factory = new HttpClientFactory(
                config,
                WebClient.builder(),
                new ObjectMapper()
        );
        assertThat(factory, notNullValue());
        assertThat(factory.get("test"), notNullValue());
        assertThat(factory.get("test"), instanceOf(HttpClientImpl.class));
    }
}
