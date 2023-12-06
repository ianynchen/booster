package io.github.booster.web.handler.compression.request.wrappers;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

public class CompressionServletInputStream extends ServletInputStream {

    private final InputStream inputStream;

    private final ServletInputStream httpServletInputStream;

    public CompressionServletInputStream(
            InputStream inputStream,
            HttpServletRequest request
    ) throws IOException {
        this.inputStream = inputStream;
        this.httpServletInputStream = request.getInputStream();
    }

    @Override
    public boolean isFinished() {
        return this.httpServletInputStream.isFinished();
    }

    @Override
    public boolean isReady() {
        return this.httpServletInputStream.isReady();
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        this.httpServletInputStream.setReadListener(readListener);
    }

    @Override
    public int read() throws IOException {
        return this.inputStream.read();
    }
}
