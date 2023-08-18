package io.github.booster.factories;

import arrow.core.Option;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.booster.commons.cache.GenericKeyedObjectCache;
import io.github.booster.commons.cache.KeyedCacheObjectFactory;
import io.github.booster.commons.cache.KeyedObjectCache;
import io.github.booster.http.client.HttpClient;
import io.github.booster.http.client.config.HttpClientConnectionConfig;
import io.github.booster.http.client.impl.HttpClientImpl;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Set;

public class HttpClientFactory
        implements KeyedCacheObjectFactory<String, HttpClient>,
        KeyedObjectCache<String, HttpClient> {

    private final HttpClientConnectionConfig config;

    private final ObjectMapper mapper;

    private final WebClient.Builder builder;

    private final KeyedObjectCache<String, HttpClient> cache;

    public HttpClientFactory(
            HttpClientConnectionConfig config,
            WebClient.Builder builder,
            ObjectMapper mapper
    ) {
        this.config = config;
        this.mapper = mapper;
        this.builder = builder;
        this.cache = new GenericKeyedObjectCache<>(this);
    }

    @NotNull
    @Override
    public Set<String> getKeys() {
        return this.cache.getKeys();
    }

    @NotNull
    @Override
    public Option<HttpClient> tryGet(String key) {
        if (key == null) {
            return null;
        }
        return this.cache.tryGet(key);
    }

    @Nullable
    @Override
    public HttpClient get(String key) {
        if (key == null) {
            return null;
        }
        return this.cache.get(key);
    }

    @Nullable
    @Override
    public HttpClient create(String key) {
        val client = this.config.create(this.builder, key);
        if (client != null) {
            return new HttpClientImpl(
                    key,
                    client,
                    this.mapper
            );
        }
        return null;
    }
}
