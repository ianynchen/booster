package io.github.booster.config.thread;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Thread pool settings
 */
@ToString
@Getter
@EqualsAndHashCode
public class ThreadPoolSetting {

    /**
     * Default pool core size
     */
    public static final int DEFAULT_CORE_SIZE = 10;

    /**
     * Default pool max size
     */
    public static final int DEFAULT_MAX_SIZE = 20;

    /**
     * Default pool queue size
     */
    public static final int DEFAULT_QUEUE_SIZE = 100;

    private int coreSize = DEFAULT_CORE_SIZE;

    private int maxSize = DEFAULT_MAX_SIZE;

    private int queueSize = DEFAULT_QUEUE_SIZE;

    private String prefix;

    /**
     * Default constructor with default values.
     */
    public ThreadPoolSetting() {
    }

    /**
     * Sets pool core size
     * @param coreSize core size need to be greater than 0, or defaults to 10
     */
    public void setCoreSize(int coreSize) {
        this.coreSize = coreSize <= 0 ? DEFAULT_CORE_SIZE : coreSize;
    }

    /**
     * Sets max size of the thread pool
     * @param maxSize max size of thread pool, need to be greater than 0 or defaults to 20
     */
    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize <= 0 ? DEFAULT_MAX_SIZE : maxSize;
    }

    /**
     * Sets queue size of the thread pool
     * @param queueSize queue size, need to be greater than 0 or defaults to 100
     */
    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize <= 0 ? DEFAULT_QUEUE_SIZE : queueSize;
    }

    /**
     * Sets prefix of thread pool
     * @param prefix prefix for thread pool
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
