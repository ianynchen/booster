package io.github.booster.web.handler.compression.response.wrappers;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

public abstract class AbstractCompressionResponseWrapper extends HttpServletResponseWrapper {

    private final ServletResponse servletResponse;

    private final ServletOutputStream servletOutputStream;

    /**
     * Constructs a response adaptor wrapping the given response.
     *
     * @param response the {@link HttpServletResponse} to be wrapped.
     * @throws IllegalArgumentException if the response is null
     */
    public AbstractCompressionResponseWrapper(
            HttpServletResponse response
    ) throws IOException {
        super(response);
        this.servletResponse = response;
        this.servletOutputStream = this.createCompressorOutputStream(response);
    }

    abstract protected ServletOutputStream createCompressorOutputStream(ServletResponse response) throws IOException;

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return this.servletOutputStream;
    }

    @Override
    public void flushBuffer() throws IOException {
        this.servletResponse.flushBuffer();
    }
}
