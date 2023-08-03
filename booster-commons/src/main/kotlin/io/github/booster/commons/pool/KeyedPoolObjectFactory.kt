package io.github.booster.commons.pool

interface KeyedPoolObjectFactory<K, V> {

    fun create(key: K): V?
}
