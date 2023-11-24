package io.github.booster.web.handler.compression.request.wrappers;

import io.github.booster.commons.compression.CompressionAlgorithm;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

public class DeflateRequestWrapper extends AbstractCompressionRequestWrapper {

    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request the {@link HttpServletRequest} to be wrapped.
     * @throws IllegalArgumentException if the request is null
     */
    public DeflateRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
    }

    @Override
    protected InputStream createCompressorInputStream() throws IOException {
        return this.createCompressorInputStream(CompressionAlgorithm.DEFLATE);
    }
}
