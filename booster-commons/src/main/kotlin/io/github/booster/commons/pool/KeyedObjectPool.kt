package io.github.booster.commons.pool

import arrow.core.Option
import org.slf4j.LoggerFactory

/**
 * A generic in-memory key/value cache to reuse objects. When
 * a request for a certain key is made, cache will first check if
 * the keyed item exists. If so, the cached item is returned, otherwise,
 * cache invokes a factory object to create an item with the specified key,
 * caches the created item, then returns to caller.
 *
 * [K] type of key object
 * [V] type of value object.
 */
interface KeyedObjectPool<K, V> {

    companion object {
        private val log = LoggerFactory.getLogger(KeyedObjectPool::class.java)
    }

    /**
     * Retrieves object by name.
     * @param key key of the object to retrieve.
     * @return [Option] of object.
     */
    fun tryGet(key: K): Option<V> {
        log.debug("booster-commons - get option for: [{}]", key)
        return Option.fromNullable(this[key])
    }

    /**
     * Returns an object if it exists in cache or can be created by [KeyedObjectPool],
     * otherwise returns null
     * @return [V]
     */
    operator fun get(key: K): V?
}
