package io.github.booster.web.handler.compression.request;

import io.github.booster.commons.compression.CompressionAlgorithm;
import io.github.booster.commons.compression.input.CompressorInputStreamFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompressionRequestDecorator extends ServerHttpRequestDecorator {

    private final static Set<String> ALLOWED_ENCODINGS =
            Set.of(
                    CompressionAlgorithm.GZIP.getAlgorithm(),
                    CompressionAlgorithm.DEFLATE.getAlgorithm(),
                    CompressionAlgorithm.BROTLI.getAlgorithm()
            );

    private final List<String> algorithms;

    public CompressionRequestDecorator(String contentEncoding, ServerHttpRequest delegate) {
        super(delegate);
        this.algorithms = this.determineCompressionAlgorithms(contentEncoding);
    }

    private List<String> determineCompressionAlgorithms(String contentEncoding) {
        if (StringUtils.isBlank(contentEncoding)) {
            return List.of();
        }

        List<String> stream = Stream.of(contentEncoding.split(","))
                .map(String::strip)
                .filter(ALLOWED_ENCODINGS::contains)
                .collect(Collectors.toList());

        List<String> reverse = new ArrayList<>();
        for (int i = stream.size() - 1; i >= 0; i--) {
            reverse.add(stream.get(i));
        }
        stream = reverse;
        return stream;
    }

    @Override
    public Flux<DataBuffer> getBody() {

        Mono<DataBuffer> buffer = DataBufferUtils.join(this.getDelegate().getBody());
        return buffer.flatMapMany(buf -> {
            InputStream inputStream = buf.asInputStream();

            for (String encoding: this.algorithms) {
                CompressionAlgorithm algorithm = CompressionAlgorithm.Companion.findAlgorithm(encoding);
                try {
                    inputStream = CompressorInputStreamFactory.INSTANCE.create(
                            algorithm,
                            inputStream
                    );
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }

            final InputStream is = inputStream;
            return DataBufferUtils.readInputStream(() -> is, new DefaultDataBufferFactory(), 100);
        });
    }
}
