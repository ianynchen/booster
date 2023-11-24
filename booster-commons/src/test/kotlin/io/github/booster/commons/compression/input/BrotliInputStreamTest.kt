package io.github.booster.commons.compression.input

import io.github.booster.commons.compression.CompressionTestData
import io.github.booster.commons.compression.getBase64DecodedBytes
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class BrotliInputStreamTest {

    @Test
    fun `should decompress`() {
        val brotliInputStream = BrotliInputStream(
            ByteArrayInputStream(getBase64DecodedBytes(CompressionTestData.BROTLI_COMPRESSED))
        )
        val result = String(brotliInputStream.readAllBytes(), StandardCharsets.UTF_8)
        assertThat(result, notNullValue())
        assertThat(result, equalTo(CompressionTestData.TEXT_TO_COMPRESS))
    }
}
