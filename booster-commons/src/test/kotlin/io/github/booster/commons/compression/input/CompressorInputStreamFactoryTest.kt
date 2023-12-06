package io.github.booster.commons.compression.input

import io.github.booster.commons.compression.CompressionAlgorithm
import io.github.booster.commons.compression.CompressionTestData
import io.github.booster.commons.compression.getBase64DecodedBytes
import io.github.booster.commons.compression.getUTF8Bytes
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.io.IOException

class CompressorInputStreamFactoryTest {

    @Test
    fun `should create input stream`() {
        assertThat(
            CompressorInputStreamFactory.create(
                CompressionAlgorithm.GZIP,
                ByteArrayInputStream(getBase64DecodedBytes(CompressionTestData.GZIP_COMPRESSED))
            ),
            notNullValue()
        )
        assertThat(
            CompressorInputStreamFactory.create(
                CompressionAlgorithm.DEFLATE,
                ByteArrayInputStream(getBase64DecodedBytes(CompressionTestData.DEFLATE_COMPRESSED))
            ),
            notNullValue()
        )
        assertThat(
            CompressorInputStreamFactory.create(
                CompressionAlgorithm.BROTLI,
                ByteArrayInputStream(getBase64DecodedBytes(CompressionTestData.BROTLI_COMPRESSED))
            ),
            notNullValue()
        )
    }

    @Test
    fun `should fail create`() {
        assertThrows<IOException> {
            CompressorInputStreamFactory.create(
                CompressionAlgorithm.COMPRESS,
                ByteArrayInputStream(getBase64DecodedBytes(CompressionTestData.LZW_COMPRESSED))
            )
        }
    }
}
