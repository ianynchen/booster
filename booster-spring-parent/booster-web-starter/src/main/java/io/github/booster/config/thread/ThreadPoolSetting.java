package io.github.booster.config.thread;

import lombok.ToString;

/**
 * Thread pool settings
 */
@ToString
public class ThreadPoolSetting {

    public static final int DEFAULT_CORE_SIZE = 10;
    public static final int DEFAULT_MAX_SIZE = 20;
    public static final int DEFAULT_QUEUE_SIZE = 100;

    private int coreSize = DEFAULT_CORE_SIZE;

    private int maxSize = DEFAULT_MAX_SIZE;

    private int queueSize = DEFAULT_QUEUE_SIZE;

    private String prefix;

    public int getCoreSize() {
        return coreSize;
    }

    public void setCoreSize(int coreSize) {
        this.coreSize = coreSize <= 0 ? DEFAULT_CORE_SIZE : coreSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize <= 0 ? DEFAULT_MAX_SIZE : maxSize;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize <= 0 ? DEFAULT_QUEUE_SIZE : queueSize;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
