package io.github.booster.web.handler.compression.request;

import io.github.booster.commons.compression.output.DeflateOutputStream;
import io.github.booster.commons.compression.output.GZipOutputStream;
import io.github.booster.web.handler.compression.CompressionTestData;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static io.github.booster.web.handler.compression.CompressionTestData.BROTLI_COMPRESSED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;

class CompressionRequestDecoratorTest {

    private MockServerHttpRequest createMockRequest(String body) {
        return MockServerHttpRequest.post("http://abc.com")
                .body(Mono.just(new DefaultDataBufferFactory().wrap(CompressionTestData.decode(body))));
    }

    @Test
    void shouldDecompressGzip() {
        MockServerHttpRequest request = createMockRequest(CompressionTestData.GZIP_COMPRESSED);
        CompressionRequestDecorator decorator = new CompressionRequestDecorator("gzip", request);
        StepVerifier.create(DataBufferUtils.join(decorator.getBody()))
                .consumeNextWith( buffer -> {
                    InputStream is = buffer.asInputStream(true);
                    try {
                        String result = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        assertThat(result, equalTo(CompressionTestData.TEXT_TO_COMPRESS));
                    } catch (IOException e) {
                        fail("unable to read all bytes");
                    }
                }).verifyComplete();
    }

    @Test
    void shouldDecompressDeflate() {
        MockServerHttpRequest request = createMockRequest(CompressionTestData.DEFLATE_COMPRESSED);
        CompressionRequestDecorator decorator = new CompressionRequestDecorator("deflate", request);
        StepVerifier.create(DataBufferUtils.join(decorator.getBody()))
                .consumeNextWith( buffer -> {
                    InputStream is = buffer.asInputStream(true);
                    try {
                        String result = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        assertThat(result, equalTo(CompressionTestData.TEXT_TO_COMPRESS));
                    } catch (IOException e) {
                        fail("unable to read all bytes");
                    }
                }).verifyComplete();
    }

    @Test
    void shouldDecompressBrotli() {
        MockServerHttpRequest request = createMockRequest(BROTLI_COMPRESSED);
        CompressionRequestDecorator decorator = new CompressionRequestDecorator("br", request);
        StepVerifier.create(DataBufferUtils.join(decorator.getBody()))
                .consumeNextWith( buffer -> {
                    InputStream is = buffer.asInputStream(true);
                    try {
                        String result = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        assertThat(result, equalTo(CompressionTestData.TEXT_TO_COMPRESS));
                    } catch (IOException e) {
                        fail("unable to read all bytes");
                    }
                }).verifyComplete();
    }

    @Test
    void shouldDecompressMultiple() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZipOutputStream gZipOutputStream = new GZipOutputStream(byteArrayOutputStream);
        DeflateOutputStream deflateOutputStream = new DeflateOutputStream(gZipOutputStream);

        deflateOutputStream.write(CompressionTestData.decode(BROTLI_COMPRESSED));
        deflateOutputStream.flush();
        deflateOutputStream.close();

        byte[] compressedContent = byteArrayOutputStream.toByteArray();
        MockServerHttpRequest request = createMockRequest(CompressionTestData.encode(compressedContent));

        CompressionRequestDecorator decorator = new CompressionRequestDecorator("br,deflate,gzip", request);
        StepVerifier.create(DataBufferUtils.join(decorator.getBody()))
                .consumeNextWith( buffer -> {
                    InputStream is = buffer.asInputStream(true);
                    try {
                        String result = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        assertThat(result, equalTo(CompressionTestData.TEXT_TO_COMPRESS));
                    } catch (IOException e) {
                        fail("unable to read all bytes");
                    }
                }).verifyComplete();
    }
}
