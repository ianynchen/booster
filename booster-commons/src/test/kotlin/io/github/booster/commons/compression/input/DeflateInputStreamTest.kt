package io.github.booster.commons.compression.input

import io.github.booster.commons.compression.CompressionTestData
import io.github.booster.commons.compression.getBase64DecodedBytes
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class DeflateInputStreamTest {

    @Test
    fun `should decompress`() {
        val deflateInputStream = DeflateInputStream(
            ByteArrayInputStream(getBase64DecodedBytes(CompressionTestData.DEFLATE_COMPRESSED))
        )
        val result = String(deflateInputStream.readAllBytes(), StandardCharsets.UTF_8)
        MatcherAssert.assertThat(result, Matchers.notNullValue())
        MatcherAssert.assertThat(result, Matchers.equalTo(CompressionTestData.TEXT_TO_COMPRESS))
    }
}
