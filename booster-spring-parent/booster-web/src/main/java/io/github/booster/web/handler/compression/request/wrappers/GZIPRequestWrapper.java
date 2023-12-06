package io.github.booster.web.handler.compression.request.wrappers;

import io.github.booster.commons.compression.CompressionAlgorithm;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

public class GZIPRequestWrapper extends AbstractCompressionRequestWrapper {

    public GZIPRequestWrapper(
            HttpServletRequest request
    ) throws IOException {
        super(request);
    }

    @Override
    protected InputStream createCompressorInputStream() throws IOException {
        return this.createCompressorInputStream(CompressionAlgorithm.GZIP);
    }
}
