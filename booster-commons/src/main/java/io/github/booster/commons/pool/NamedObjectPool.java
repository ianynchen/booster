package io.github.booster.commons.pool;

import arrow.core.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Object pool that allows objects to be retrieved by names.
 *
 * @param <T> type of object pooled.
 */
public abstract class NamedObjectPool<T> {

    private static final Logger log = LoggerFactory.getLogger(NamedObjectPool.class);

    /**
     * Cached objects.
     */
    private final Map<String, T> cachedObjects = new HashMap<>();

    abstract protected T createObject(String name);

    /**
     * Retrieves object by name.
     * @param name name of object to retrieve.
     * @return {@link Option} of object.
     */
    public Option<T> getOption(String name) {
        log.debug("booster-commons - get optional for: [{}]", name);
        return Option.fromNullable(this.get(name));
    }

    protected Set<String> getKeys() {
        synchronized (this.cachedObjects) {
            return this.cachedObjects.keySet();
        }
    }

    /**
     * Retrieves object by name.
     * @param name name of object to retrieve.
     * @return Returns object of the name, or null if object cannot be found and cannot be created.
     */
    public T get(String name) {
        synchronized(this.cachedObjects) {
            log.debug("booster-commons - get object for: [{}]", name);
            if (this.cachedObjects.containsKey(name)) {
                log.debug("booster-commons - named object: [{}] exists in cache", name);
                return this.cachedObjects.get(name);
            } else {
                log.debug("booster-commons - creating named object: [{}]", name);
                T obj = this.createObject(name);
                if (obj != null) {
                    log.debug("booster-commons - named object: [{}] created", name);
                    this.cachedObjects.put(name, obj);
                }
                log.debug("booster-commons - named object: [{}] not created", name);
                return obj;
            }
        }
    }
}
