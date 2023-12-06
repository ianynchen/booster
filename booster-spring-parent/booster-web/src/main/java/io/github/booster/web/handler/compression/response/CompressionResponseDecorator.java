package io.github.booster.web.handler.compression.response;

import io.github.booster.commons.compression.CompressionAlgorithm;
import io.github.booster.commons.compression.output.CompressorOutputStreamFactory;
import io.github.booster.web.handler.compression.AcceptEncodingParser;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

public class CompressionResponseDecorator extends ServerHttpResponseDecorator {

    private final static Logger log = LoggerFactory.getLogger(CompressionResponseDecorator.class);

    private final static Set<String> SUPPORTED_COMPRESSION_ENCODINGS =
            Set.of(
                    CompressionAlgorithm.DEFLATE.getAlgorithm(),
                    CompressionAlgorithm.NONE.getAlgorithm(),
                    CompressionAlgorithm.GZIP.getAlgorithm()
            );

    private final OutputStream outputStream;

    @Getter
    private final CompressionAlgorithm algorithm;

    private final DataBuffer buffer;

    private final ByteArrayOutputStream byteArrayOutputStream;

    private final DataBufferFactory dataBufferFactory;

    public CompressionResponseDecorator(String acceptEncoding, ServerHttpResponse delegate) {
        super(delegate);
        this.algorithm = AcceptEncodingParser.findCompressionAlgorithm(acceptEncoding, SUPPORTED_COMPRESSION_ENCODINGS);
        this.dataBufferFactory = new DefaultDataBufferFactory();
        this.buffer = dataBufferFactory.allocateBuffer(1024);
        this.byteArrayOutputStream = new ByteArrayOutputStream(1024);
        this.outputStream = this.createStream();
    }

    private OutputStream createStream() {

        OutputStream stream = this.byteArrayOutputStream;
        try {
            switch (algorithm) {
                case GZIP:
                    stream = CompressorOutputStreamFactory.INSTANCE
                            .create(CompressionAlgorithm.GZIP, this.byteArrayOutputStream);
                    break;
                case COMPRESS:
                    stream = CompressorOutputStreamFactory.INSTANCE
                            .create(CompressionAlgorithm.COMPRESS, this.byteArrayOutputStream);
                    break;
                case DEFLATE:
                    stream = CompressorOutputStreamFactory.INSTANCE
                            .create(CompressionAlgorithm.DEFLATE, this.byteArrayOutputStream);
                    break;
                case BROTLI:
                    stream = CompressorOutputStreamFactory.INSTANCE
                            .create(CompressionAlgorithm.BROTLI, this.byteArrayOutputStream);
                    break;
            }
        } catch (IOException e) {
            log.warn("booster-web - cannot create compressor output stream for accept-encoding: {}", algorithm, e);
        }
        return stream;
    }

    private DataBuffer compress(DataBuffer dataBuffer) {
        byte[] data = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(data);
        try {
            try {
                this.outputStream.write(data);
            } finally {
                this.outputStream.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                this.byteArrayOutputStream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        byte[] compressedData = this.byteArrayOutputStream.toByteArray();

        DataBufferFactory bufferFactory = dataBuffer.factory();
        DataBuffer buffer = bufferFactory.allocateBuffer(compressedData.length);
        buffer.write(compressedData);

        return buffer;
    }

    @NotNull
    @Override
    public Mono<Void> writeWith(@NotNull Publisher<? extends DataBuffer> body) {
        return super.writeWith(Flux.from(body).map(this::compress));
    }

    @NotNull
    @Override
    public Mono<Void> writeAndFlushWith(@NotNull Publisher<? extends Publisher<? extends DataBuffer>> body) {
        Flux<Publisher<? extends DataBuffer>> flux = Flux.from(body);
        Flux<DataBuffer> buffers = flux.flatMap(Flux::from);
        return this.writeWith(buffers);
    }

    @NotNull
    @Override
    public Mono<Void> setComplete() {
        return super.setComplete();
    }

    public boolean canCompress() {
        return this.outputStream != null;
    }
}
