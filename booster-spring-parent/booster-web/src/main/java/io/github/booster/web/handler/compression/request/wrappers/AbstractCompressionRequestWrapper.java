package io.github.booster.web.handler.compression.request.wrappers;

import io.github.booster.commons.compression.CompressionAlgorithm;
import io.github.booster.commons.compression.input.CompressorInputStreamFactory;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.io.InputStream;

public abstract class AbstractCompressionRequestWrapper extends HttpServletRequestWrapper {

    private final InputStream compressorInputStream;

    private final HttpServletRequest httpServletRequest;

    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request the {@link HttpServletRequest} to be wrapped.
     * @throws IllegalArgumentException if the request is null
     */
    public AbstractCompressionRequestWrapper(
            HttpServletRequest request
    ) throws IOException {
        super(request);
        this.compressorInputStream = this.createCompressorInputStream();
        this.httpServletRequest = request;
    }

    protected abstract InputStream createCompressorInputStream() throws IOException;

    protected InputStream createCompressorInputStream(
            CompressionAlgorithm algorithm
    ) throws IOException {
        return CompressorInputStreamFactory.INSTANCE.create(
                algorithm,
                super.getInputStream()
        );
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new CompressionServletInputStream(this.compressorInputStream, this.httpServletRequest);
    }
}
