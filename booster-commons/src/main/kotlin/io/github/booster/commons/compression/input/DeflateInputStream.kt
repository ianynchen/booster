package io.github.booster.commons.compression.input

import io.github.booster.commons.compression.CompressionAlgorithm
import java.io.InputStream

class DeflateInputStream(
    inputStream: InputStream
) : AbstractCompressorInputStream(inputStream) {

    override fun createCompressorInputStream(inputStream: InputStream): InputStream =
        this.createCompressorInputStream(CompressionAlgorithm.DEFLATE, inputStream)
}
