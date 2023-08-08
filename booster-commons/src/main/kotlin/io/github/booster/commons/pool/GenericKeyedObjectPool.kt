package io.github.booster.commons.pool

import org.slf4j.LoggerFactory

/**
 * Object pool that allows objects to be retrieved by names.
 * @param <T> type of object pooled.
 */
class GenericKeyedObjectPool<K, V>(
    private val factory: KeyedPoolObjectFactory<K, V>
): KeyedObjectPool<K, V> {
    /**
     * Cached objects.
     */
    private val cachedObjects: MutableMap<K, V> = mutableMapOf()

    private val keys: Set<K>
        get() {
            synchronized(cachedObjects) { return cachedObjects.keys }
        }

    /**
     * Retrieves object by name.
     * @param key name of object to retrieve.
     * @return Returns object of the name, or null if object cannot be found and cannot be created.
     */
    override operator fun get(key: K): V? {
        synchronized(this.cachedObjects) {
            log.debug("booster-commons - get object for: [{}]", key)
            return if (cachedObjects.containsKey(key)) {
                log.debug("booster-commons - named object: [{}] exists in cache", key)
                this.cachedObjects[key]
            } else {
                log.debug("booster-commons - creating named object: [{}]", key)
                val obj: V? = this.factory.create(key)
                if (obj != null) {
                    log.debug("booster-commons - named object: [{}] created", key)
                    this.cachedObjects[key] = obj
                }
                log.debug("booster-commons - named object: [{}] not created", key)
                obj
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(GenericKeyedObjectPool::class.java)
    }
}
