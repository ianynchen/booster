package io.github.booster.commons.compression.input

import io.github.booster.commons.compression.CompressionAlgorithm
import java.io.IOException
import java.io.InputStream

object CompressorInputStreamFactory {

    @Throws(IOException::class)
    fun create(algorithm: CompressionAlgorithm, inputStream: InputStream) =
        when(algorithm) {
            CompressionAlgorithm.DEFLATE -> DeflateInputStream(inputStream)
            CompressionAlgorithm.GZIP -> GZipInputStream(inputStream)
            CompressionAlgorithm.COMPRESS -> throw IOException("unknown compression algorithm")
            CompressionAlgorithm.BROTLI -> BrotliInputStream(inputStream)
            CompressionAlgorithm.NONE -> inputStream
        }
}
