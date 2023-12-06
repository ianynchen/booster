package io.github.booster.commons.compression

enum class CompressionAlgorithm(val algorithm: String) {
    DEFLATE("deflate"),
    COMPRESS("compress"),
    GZIP("gzip"),
    BROTLI("br"),
    NONE("");

    companion object {
        fun findAlgorithm(name: String): CompressionAlgorithm {
            for (value in values()) {
                if (value.algorithm == name) {
                    return value
                }
            }
            return NONE
        }
    }
}
