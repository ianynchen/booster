package io.github.booster.commons.compression.output

import io.github.booster.commons.compression.CompressionTestData
import io.github.booster.commons.compression.getBase64EncodedString
import io.github.booster.commons.compression.getUTF8Bytes
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.greaterThan
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class DeflateOutputStreamTest {

    @Test
    fun `should compress`() {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val outputStream = DeflateOutputStream(byteArrayOutputStream)

        outputStream.write(getUTF8Bytes(CompressionTestData.TEXT_TO_COMPRESS))

        outputStream.flush()
        outputStream.close()
        val result = getBase64EncodedString(byteArrayOutputStream.toByteArray())
        assertThat(result, notNullValue())
        assertThat(result.length, greaterThan(0))
        assertThat(result, equalTo(CompressionTestData.DEFLATE_COMPRESSED))
    }
}
