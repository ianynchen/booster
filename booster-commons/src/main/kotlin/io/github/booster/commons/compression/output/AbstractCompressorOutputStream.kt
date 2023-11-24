package io.github.booster.commons.compression.output

import io.github.booster.commons.compression.CompressionAlgorithm
import org.apache.commons.compress.compressors.CompressorException
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.io.IOException
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

abstract class AbstractCompressorOutputStream(
    private val os: OutputStream
) : OutputStream() {
    private val outputStream: OutputStream

    init {
        outputStream = this.createOutputStream(os)
    }

    @Throws(IOException::class)
    protected abstract fun createOutputStream(os: OutputStream): OutputStream

    @Throws(IOException::class)
    protected fun createOutputStream(
        os: OutputStream,
        algorithm: CompressionAlgorithm
    ): OutputStream {
        return when (algorithm) {
            CompressionAlgorithm.GZIP -> GZIPOutputStream(os)
            CompressionAlgorithm.DEFLATE, CompressionAlgorithm.BROTLI, CompressionAlgorithm.COMPRESS ->
                try {
                    CompressorStreamFactory.getSingleton()
                        .createCompressorOutputStream(
                            algorithm.algorithm,
                            os
                        )
                } catch (e: CompressorException) {
                    throw IOException("unknown compression algorithm: " + algorithm.algorithm, e)
                }
            CompressionAlgorithm.NONE -> os
        }
    }

    @Throws(IOException::class)
    override fun write(b: Int) {
        outputStream.write(b)
    }

    @Throws(IOException::class)
    override fun flush() {
        outputStream.flush()
        os.flush()
    }

    @Throws(IOException::class)
    override fun close() {
        outputStream.close()
        os.close()
    }
}
