package io.github.booster.http.client.request;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class UserContextTest {

    @Test
    void shouldCreateEmptyMap() {
        Map<String, String> headers = UserContext.builder().build().createHeaders();
        assertThat(headers, notNullValue());
        assertThat(headers.size(), equalTo(0));
    }

    @Test
    void shouldCreateDeviceType() {
        Map<String, String> headers = UserContext.builder()
                .deviceType("WEB")
                .build()
                .createHeaders();
        assertThat(headers, notNullValue());
        assertThat(headers.size(), equalTo(1));
        assertThat(headers.keySet(), contains(UserContext.DEVICE_TYPE_HEADER));
        assertThat(headers.values(), contains("WEB"));
    }

    @Test
    void shouldCreateAcceptLanguage() {
        Map<String, String> headers = UserContext.builder()
                .acceptLanguage("en")
                .build()
                .createHeaders();
        assertThat(headers, notNullValue());
        assertThat(headers.size(), equalTo(1));
        assertThat(headers.keySet(), contains(UserContext.ACCEPT_LANGUAGE_HEADER));
        assertThat(headers.values(), contains("en"));
    }

    @Test
    void shouldCreateBusinessUserAgent() {
        Map<String, String> headers = UserContext.builder()
                .businessAgent("promo-engine")
                .build()
                .createHeaders();
        assertThat(headers, notNullValue());
        assertThat(headers.size(), equalTo(1));
        assertThat(headers.keySet(), contains(UserContext.BUSINESS_AGENT_HEADER));
        assertThat(headers.values(), contains("promo-engine"));
    }

    @Test
    void shouldCreateTenant() {
        Map<String, String> headers = UserContext.builder()
                .tenant("JOE_FRESH")
                .build()
                .createHeaders();
        assertThat(headers, notNullValue());
        assertThat(headers.size(), equalTo(1));
        assertThat(headers.keySet(), contains(UserContext.TENANT_HEADER));
        assertThat(headers.values(), contains("JOE_FRESH"));
    }
}
