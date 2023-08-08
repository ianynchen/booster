package io.github.booster.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.booster.commons.pool.GenericKeyedObjectPool;
import io.github.booster.http.client.HttpClient;
import io.github.booster.http.client.config.HttpClientConnectionConfig;
import io.github.booster.http.client.impl.HttpClientImpl;
import lombok.val;
import org.springframework.web.reactive.function.client.WebClient;

public class HttpClientFactory extends GenericKeyedObjectPool<HttpClient> {

    private final HttpClientConnectionConfig config;

    private final ObjectMapper mapper;

    private final WebClient.Builder builder;

    public HttpClientFactory(
            HttpClientConnectionConfig config,
            WebClient.Builder builder,
            ObjectMapper mapper
    ) {
        this.config = config;
        this.mapper = mapper;
        this.builder = builder;
    }

    @Override
    protected HttpClient createObject(String name) {
        val client = this.config.create(this.builder, name);
        if (client != null) {
            return new HttpClientImpl(
                    name,
                    client,
                    this.mapper
            );
        }
        return null;
    }
}
