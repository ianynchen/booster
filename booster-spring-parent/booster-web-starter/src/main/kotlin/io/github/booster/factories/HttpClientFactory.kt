package io.github.booster.factories

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.booster.commons.cache.GenericKeyedObjectCache
import io.github.booster.commons.cache.KeyedCacheObjectFactory
import io.github.booster.commons.cache.KeyedObjectCache
import io.github.booster.http.client.HttpClient
import io.github.booster.http.client.config.HttpClientConnectionConfig
import io.github.booster.http.client.impl.HttpClientImpl
import org.springframework.web.reactive.function.client.WebClient

class HttpClientFactory(
    config: HttpClientConnectionConfig,
    builder: WebClient.Builder,
    mapper: ObjectMapper
) : KeyedCacheObjectFactory<String, HttpClient<*, *>>, KeyedObjectCache<String, HttpClient<*, *>> {

    private val config: HttpClientConnectionConfig
    private val mapper: ObjectMapper
    private val builder: WebClient.Builder
    private val pool: GenericKeyedObjectCache<String, HttpClient<*, *>>

    init {
        this.config = config
        this.mapper = mapper
        this.builder = builder
        this.pool = GenericKeyedObjectCache(this)
    }

    override fun create(key: String): HttpClient<*, *> {
        val client: WebClient = config.create(builder, key)
        return HttpClientImpl<Any?, Any?>(
            key,
            client,
            mapper
        )
    }

    override fun getKeys(): Set<String> = this.pool.getKeys()

    override fun get(key: String): HttpClient<*, *>? = this.pool[key]
}
