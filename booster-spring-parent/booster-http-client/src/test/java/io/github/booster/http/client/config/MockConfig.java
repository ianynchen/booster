package io.github.booster.http.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MockConfig {

    @Bean
    public CustomWebClientExchangeTagsProvider clientExchangeTagsProvider() {
        return new CustomWebClientExchangeTagsProvider();
    }
}
