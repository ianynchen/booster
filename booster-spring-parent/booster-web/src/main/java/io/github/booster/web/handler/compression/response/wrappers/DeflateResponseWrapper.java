package io.github.booster.web.handler.compression.response.wrappers;

import io.github.booster.commons.compression.CompressionAlgorithm;
import io.github.booster.commons.compression.output.CompressorOutputStreamFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class DeflateResponseWrapper extends AbstractCompressionResponseWrapper {

    /**
     * Constructs a response adaptor wrapping the given response.
     *
     * @param response the {@link HttpServletResponse} to be wrapped.
     * @throws IllegalArgumentException if the response is null
     */
    public DeflateResponseWrapper(HttpServletResponse response) throws IOException {
        super(response);
    }

    @Override
    protected ServletOutputStream createCompressorOutputStream(ServletResponse response) throws IOException {
        return new CompressionServletOutputStream(
                CompressorOutputStreamFactory.INSTANCE.create(CompressionAlgorithm.DEFLATE, response.getOutputStream()),
                response
        );
    }
}
