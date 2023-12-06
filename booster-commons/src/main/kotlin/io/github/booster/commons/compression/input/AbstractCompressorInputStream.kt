package io.github.booster.commons.compression.input

import io.github.booster.commons.compression.CompressionAlgorithm
import org.apache.commons.compress.compressors.brotli.BrotliCompressorInputStream
import org.apache.commons.compress.compressors.z.ZCompressorInputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

abstract class AbstractCompressorInputStream(
    private val inputStream: InputStream
) : InputStream() {
    private var compressorInputStream: InputStream

    init {
        compressorInputStream = this.createCompressorInputStream(inputStream)
    }

    @Throws(IOException::class)
    protected fun createCompressorInputStream(
        algorithm: CompressionAlgorithm,
        inputStream: InputStream
    ): InputStream {
        return when (algorithm) {
            CompressionAlgorithm.COMPRESS -> ZCompressorInputStream(inputStream)
            CompressionAlgorithm.BROTLI -> BrotliCompressorInputStream(inputStream)
            CompressionAlgorithm.DEFLATE -> InflaterInputStream(inputStream, Inflater())
            CompressionAlgorithm.GZIP -> GZIPInputStream(inputStream)
            CompressionAlgorithm.NONE -> inputStream
        }
    }

    @Throws(IOException::class)
    protected abstract fun createCompressorInputStream(inputStream: InputStream): InputStream

    @Throws(IOException::class)
    override fun read(): Int {
        return compressorInputStream.read()
    }

    @Throws(IOException::class)
    override fun available(): Int {
        return compressorInputStream.available()
    }

    @Throws(IOException::class)
    override fun close() {
        compressorInputStream.close()
        this.inputStream.close()
    }
}
