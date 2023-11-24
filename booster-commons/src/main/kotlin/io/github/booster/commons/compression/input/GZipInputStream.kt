package io.github.booster.commons.compression.input

import io.github.booster.commons.compression.CompressionAlgorithm
import java.io.InputStream

class GZipInputStream(inputStream: InputStream)
    : AbstractCompressorInputStream(inputStream) {

    override fun createCompressorInputStream(inputStream: InputStream): InputStream =
        this.createCompressorInputStream(CompressionAlgorithm.GZIP, inputStream)
}
