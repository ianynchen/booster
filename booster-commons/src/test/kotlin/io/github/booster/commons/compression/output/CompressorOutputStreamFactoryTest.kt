package io.github.booster.commons.compression.output

import io.github.booster.commons.compression.CompressionAlgorithm
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayOutputStream
import java.io.IOException

class CompressorOutputStreamFactoryTest {

    @Test
    fun `should create output stream`() {
        assertThat(
            CompressorOutputStreamFactory.create(
                CompressionAlgorithm.DEFLATE,
                ByteArrayOutputStream()
            ),
            notNullValue()
        )
        assertThat(
            CompressorOutputStreamFactory.create(
                CompressionAlgorithm.GZIP,
                ByteArrayOutputStream()
            ),
            notNullValue()
        )
    }

    @Test
    fun `should fail create output stream`() {
        assertThrows<IOException> {
            CompressorOutputStreamFactory.create(CompressionAlgorithm.COMPRESS, ByteArrayOutputStream())
        }
        assertThrows<IOException> {
            CompressorOutputStreamFactory.create(CompressionAlgorithm.BROTLI, ByteArrayOutputStream())
        }
    }
}
