package io.github.booster.commons.compression.input

import io.github.booster.commons.compression.CompressionAlgorithm
import java.io.InputStream

class CompressInputStream(
    inputStream: InputStream
) : AbstractCompressorInputStream(inputStream) {

    override fun createCompressorInputStream(inputStream: InputStream) =
        this.createCompressorInputStream(CompressionAlgorithm.COMPRESS, inputStream)
}
