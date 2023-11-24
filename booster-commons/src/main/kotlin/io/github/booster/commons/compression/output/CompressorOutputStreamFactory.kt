package io.github.booster.commons.compression.output

import io.github.booster.commons.compression.CompressionAlgorithm
import java.io.IOException
import java.io.OutputStream

object CompressorOutputStreamFactory {

    @Throws(IOException::class)
    fun create(algorithm: CompressionAlgorithm, outputStream: OutputStream) =
        when(algorithm) {
            CompressionAlgorithm.DEFLATE -> DeflateOutputStream(outputStream)
            CompressionAlgorithm.GZIP -> GZipOutputStream(outputStream)
            CompressionAlgorithm.COMPRESS -> CompressOutputStream(outputStream)
            CompressionAlgorithm.BROTLI -> BrotliOutputStream(outputStream)
            CompressionAlgorithm.NONE -> outputStream
        }
}
