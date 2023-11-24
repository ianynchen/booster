package io.github.booster.web.handler.compression.response.wrappers;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Wraps an {@link OutputStream} that produces compressed data
 * as a {@link ServletOutputStream}
 */
public class CompressionServletOutputStream extends ServletOutputStream {

    private final OutputStream outputStream;

    private final ServletOutputStream servletOutputStream;

    public CompressionServletOutputStream(
        OutputStream outputStream,
        ServletResponse servletResponse
    ) throws IOException {
        this.outputStream = outputStream;
        this.servletOutputStream = servletResponse.getOutputStream();
    }

    @Override
    public boolean isReady() {
        return this.servletOutputStream.isReady();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        this.servletOutputStream.setWriteListener(writeListener);
    }

    @Override
    public void write(int b) throws IOException {
        this.outputStream.write(b);
    }

    @Override
    public void flush() throws IOException {
        this.outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        this.outputStream.close();
    }
}
