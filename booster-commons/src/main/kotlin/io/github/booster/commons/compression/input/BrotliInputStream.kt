package io.github.booster.commons.compression.input

import io.github.booster.commons.compression.CompressionAlgorithm
import java.io.InputStream

class BrotliInputStream(
    inputStream: InputStream
) : AbstractCompressorInputStream(inputStream) {

    override fun createCompressorInputStream(inputStream: InputStream) =
        this.createCompressorInputStream(CompressionAlgorithm.BROTLI, inputStream)
}
