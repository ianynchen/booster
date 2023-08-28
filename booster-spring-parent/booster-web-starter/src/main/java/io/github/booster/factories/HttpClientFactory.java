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

/**
 * Factory that creates {@link HttpClient}
 */
public class HttpClientFactory
        implements KeyedCacheObjectFactory<String, HttpClient>,
        KeyedObjectCache<String, HttpClient> {

    private final HttpClientConnectionConfig config;

    private final ObjectMapper mapper;

    private final WebClient.Builder builder;

    private final KeyedObjectCache<String, HttpClient> cache;

    /**
     * Constructs {@link HttpClientFactory}
     * @param config {@link HttpClientConnectionConfig} to create {@link HttpClient} from
     * @param builder {@link WebClient.Builder} for traces
     * @param mapper {@link ObjectMapper} for object serialization and deserialization
     */
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

    /**
     * Gets all the keys of cached {@link HttpClient}
     * @return {@link Set} of keys for all {@link HttpClient} in cache
     */
    @NotNull
    @Override
    public Set<String> getKeys() {
        return this.cache.getKeys();
    }

    /**
     * Try get an {@link HttpClient}
     * @param key key for the client of interest
     * @return {@link Option} of {@link HttpClient}
     */
    @NotNull
    @Override
    public Option<HttpClient> tryGet(String key) {
        if (key == null) {
            return null;
        }
        return this.cache.tryGet(key);
    }

    /**
     * Get an {@link HttpClient} from cache
     * @param key {@link HttpClient} of interest
     * @return {@link HttpClient} or null if key is null or no entry in cache.
     */
    @Nullable
    @Override
    public HttpClient get(String key) {
        if (key == null) {
            return null;
        }
        return this.cache.get(key);
    }

    /**
     * Creates an {@link HttpClient}
     * @param key key for {@link HttpClient}
     * @return {@link HttpClient} or null if cannot be created
     */
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
