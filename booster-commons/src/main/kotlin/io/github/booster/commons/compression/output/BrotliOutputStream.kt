package io.github.booster.commons.compression.output

import io.github.booster.commons.compression.CompressionAlgorithm
import java.io.OutputStream

class BrotliOutputStream(
    outputStream: OutputStream
) : AbstractCompressorOutputStream(outputStream) {

    override fun createOutputStream(os: OutputStream) =
        this.createOutputStream(os, CompressionAlgorithm.BROTLI)
}
