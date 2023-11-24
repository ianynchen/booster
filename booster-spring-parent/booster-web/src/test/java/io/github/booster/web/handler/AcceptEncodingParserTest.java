package io.github.booster.web.handler;

import arrow.core.Option;
import io.github.booster.commons.compression.CompressionAlgorithm;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class AcceptEncodingParserTest {

    @Test
    void shouldReturnSingleEntry() {

        List<AcceptEncodingParser.Encoding> encodings = List.of(
                new AcceptEncodingParser.Encoding(CompressionAlgorithm.NONE, 0.8),
                new AcceptEncodingParser.Encoding(CompressionAlgorithm.NONE, 0.5),
                new AcceptEncodingParser.Encoding(CompressionAlgorithm.NONE, 1.0)
        );

        Option<AcceptEncodingParser.Encoding> encoding = AcceptEncodingParser.findMaxWeight(
                encodings
        );

        assertThat(
                encoding,
                notNullValue()
        );
        assertThat(
                encoding.isDefined(),
                equalTo(true)
        );
        assertThat(
                encoding.orNull().getWeight(),
                equalTo(1.0)
        );
    }

    @Test
    void shouldParseSingleEntry() {
        assertThat(
                AcceptEncodingParser.findCompressionAlgorithm(null, Set.of("", "br", "gz")),
                equalTo(CompressionAlgorithm.NONE)
        );
        assertThat(
                AcceptEncodingParser.findCompressionAlgorithm("", Set.of("", "br", "gz")),
                equalTo(CompressionAlgorithm.NONE)
        );
        assertThat(
                AcceptEncodingParser.findCompressionAlgorithm(null, Set.of("*", "br", "gz")),
                equalTo(CompressionAlgorithm.NONE)
        );
        assertThat(
                AcceptEncodingParser.findCompressionAlgorithm("deflate", Set.of("*", "br", "gz")),
                equalTo(CompressionAlgorithm.NONE)
        );
        assertThat(
                AcceptEncodingParser.findCompressionAlgorithm("gz ; q = 0.8", Set.of("*", "br", "gz")),
                equalTo(CompressionAlgorithm.GZIP)
        );
    }

    @Test
    void shouldParseMultipleEntry() {
        assertThat(
                AcceptEncodingParser.findCompressionAlgorithm("gz;q=0.8,deflate;q=1.0", Set.of("", "br", "gz")),
                equalTo(CompressionAlgorithm.GZIP)
        );
        assertThat(
                AcceptEncodingParser.findCompressionAlgorithm("gz;q=0.8,deflate;q=1.0", Set.of("", "br", "gz", "deflate")),
                equalTo(CompressionAlgorithm.DEFLATE)
        );
    }
}
