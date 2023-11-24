package io.github.booster.commons.compression

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class CompressionAlgorithmTest {

    @Test
    fun `should find algorithm`() {
        assertThat(
            CompressionAlgorithm.findAlgorithm(CompressionAlgorithm.COMPRESS.algorithm),
            equalTo(CompressionAlgorithm.COMPRESS)
        )
        assertThat(
            CompressionAlgorithm.findAlgorithm(CompressionAlgorithm.GZIP.algorithm),
            equalTo(CompressionAlgorithm.GZIP)
        )
        assertThat(
            CompressionAlgorithm.findAlgorithm(CompressionAlgorithm.BROTLI.algorithm),
            equalTo(CompressionAlgorithm.BROTLI)
        )
        assertThat(
            CompressionAlgorithm.findAlgorithm(CompressionAlgorithm.DEFLATE.algorithm),
            equalTo(CompressionAlgorithm.DEFLATE)
        )
    }

    @Test
    fun `should not find`() {
        assertThat(
            CompressionAlgorithm.findAlgorithm("abc"),
            equalTo(CompressionAlgorithm.NONE)
        )
    }
}
