package io.github.booster.config.thread

import lombok.ToString

const val DEFAULT_CORE_SIZE = 10
const val DEFAULT_MAX_SIZE = 20
const val DEFAULT_QUEUE_SIZE = 100

@ToString
data class ThreadPoolSetting (
    val coreSize: Int?,
    val maxSize: Int?,
    val queueSize: Int?,
    val prefix: String?
) {
    fun validate(name: String): ValidatedThreadPoolSetting {
        var coreSize = if (this.coreSize != null && this.coreSize > 0) {
            this.coreSize
        } else {
            DEFAULT_CORE_SIZE
        }
        var maxSize = if (this.maxSize != null && this.maxSize > 0) {
            this.maxSize
        } else {
            DEFAULT_MAX_SIZE
        }
        var queueSize = if (this.queueSize != null && this.queueSize > 0) {
            this.queueSize
        } else {
            DEFAULT_QUEUE_SIZE
        }
        val prefix = if (this.prefix?.isNotBlank() == true) {
            this.prefix
        } else {
            name
        }

        if (coreSize > maxSize) {
            coreSize = maxSize.apply { maxSize = coreSize }
        }

        return ValidatedThreadPoolSetting(
            coreSize, maxSize, queueSize, prefix
        )
    }
}

data class ValidatedThreadPoolSetting (
    val coreSize: Int,
    val maxSize: Int,
    val queueSize: Int,
    val prefix: String
)
