package io.github.booster.commons.cache

interface KeyedCacheObjectFactory<K, V> {

    fun create(key: K): V?
}
