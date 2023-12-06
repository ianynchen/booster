package io.github.booster.commons.compression.output

import io.github.booster.commons.compression.CompressionAlgorithm
import java.io.OutputStream

class DeflateOutputStream(
    outputStream: OutputStream
) : AbstractCompressorOutputStream(outputStream) {

    override fun createOutputStream(os: OutputStream) =
        this.createOutputStream(os, CompressionAlgorithm.DEFLATE)
}
