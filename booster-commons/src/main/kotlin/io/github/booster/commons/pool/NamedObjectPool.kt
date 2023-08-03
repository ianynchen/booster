package io.github.booster.commons.pool

import arrow.core.Option
import org.slf4j.LoggerFactory

/**
 * Object pool that allows objects to be retrieved by names.
 * @param <T> type of object pooled.
 */
abstract class NamedObjectPool<T> {
    /**
     * Cached objects.
     */
    private val cachedObjects: MutableMap<String, T> = mutableMapOf()
    protected abstract fun createObject(name: String?): T

    /**
     * Retrieves object by name.
     * @param name name of object to retrieve.
     * @return [Option] of object.
     */
    fun getOption(name: String): Option<T> {
        log.debug("booster-commons - get optional for: [{}]", name)
        return Option.fromNullable<T>(this[name])
    }

    protected val keys: Set<String>
        get() {
            synchronized(cachedObjects) { return cachedObjects.keys }
        }

    /**
     * Retrieves object by name.
     * @param name name of object to retrieve.
     * @return Returns object of the name, or null if object cannot be found and cannot be created.
     */
    operator fun get(name: String): T? {
        synchronized(cachedObjects) {
            log.debug("booster-commons - get object for: [{}]", name)
            return if (cachedObjects.containsKey(name)) {
                log.debug("booster-commons - named object: [{}] exists in cache", name)
                cachedObjects[name]
            } else {
                log.debug("booster-commons - creating named object: [{}]", name)
                val obj: T? = createObject(name)
                if (obj != null) {
                    log.debug("booster-commons - named object: [{}] created", name)
                    cachedObjects[name] = obj
                }
                log.debug("booster-commons - named object: [{}] not created", name)
                obj
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(NamedObjectPool::class.java)
    }
}
