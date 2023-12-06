package io.github.booster.web.handler.compression.response;

import io.github.booster.web.handler.compression.CompressionTestData;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.mock.http.server.reactive.MockServerHttpResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;

class CompressionResponseDecoratorTest {

    private Mono<DataBuffer> createBufferFromByteArray(byte[] bytes) {
        return Mono.just(new DefaultDataBufferFactory().wrap(bytes));
    }

    private Mono<DataBuffer> createBufferFromString(String content) {
        return this.createBufferFromByteArray(CompressionTestData.getUtfBytes(content));
    }

    @Test
    void shouldCompressGzip() {
        MockServerHttpResponse response = new MockServerHttpResponse();
        CompressionResponseDecorator decorator = new CompressionResponseDecorator(
                "gzip",
                response
        );
        Mono<Void> result = decorator.writeAndFlushWith(Mono.just(this.createBufferFromString(CompressionTestData.TEXT_TO_COMPRESS)));
        StepVerifier.create(result).verifyComplete();
        decorator.setComplete().block();
        Flux<DataBuffer> buffer = response.getBody();
        DataBuffer buf = DataBufferUtils.join(buffer).block();
        InputStream is = buf.asInputStream(true);
        try {
            byte[] bytes = is.readAllBytes();
            String compressed = CompressionTestData.encode(bytes);
            assertThat(compressed, equalTo(CompressionTestData.GZIP_COMPRESSED));
        } catch (IOException e) {
            fail("cannot read input stream");
        }
    }

    @Test
    void shouldCompressDeflate() {
        MockServerHttpResponse response = new MockServerHttpResponse();
        CompressionResponseDecorator decorator = new CompressionResponseDecorator(
                "deflate",
                response
        );
        Mono<Void> result = decorator.writeAndFlushWith(Mono.just(this.createBufferFromString(CompressionTestData.TEXT_TO_COMPRESS)));
        StepVerifier.create(result).verifyComplete();
        decorator.setComplete().block();
        Flux<DataBuffer> buffer = response.getBody();
        DataBuffer buf = DataBufferUtils.join(buffer).block();
        InputStream is = buf.asInputStream(true);
        try {
            byte[] bytes = is.readAllBytes();
            String compressed = CompressionTestData.encode(bytes);
            assertThat(compressed, equalTo(CompressionTestData.DEFLATE_COMPRESSED));
        } catch (IOException e) {
            fail("cannot read input stream");
        }
    }

    @Test
    void shouldNotCompress() {
        MockServerHttpResponse response = new MockServerHttpResponse();
        CompressionResponseDecorator decorator = new CompressionResponseDecorator(
                "br,compress",
                response
        );
        Mono<Void> result = decorator.writeAndFlushWith(Mono.just(this.createBufferFromString(CompressionTestData.TEXT_TO_COMPRESS)));
        StepVerifier.create(result).verifyComplete();
        decorator.setComplete().block();
        Flux<DataBuffer> buffer = response.getBody();
        DataBuffer buf = DataBufferUtils.join(buffer).block();
        InputStream is = buf.asInputStream(true);
        try {
            byte[] bytes = is.readAllBytes();
            String compressed = new String(bytes, StandardCharsets.UTF_8);
            assertThat(compressed, equalTo(CompressionTestData.TEXT_TO_COMPRESS));
        } catch (IOException e) {
            fail("cannot read input stream");
        }
    }
}
