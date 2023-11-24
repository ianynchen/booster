package io.github.booster.web.handler.compression;

import io.github.booster.commons.compression.CompressionAlgorithm;
import io.github.booster.web.handler.AcceptEncodingParser;
import io.github.booster.web.handler.compression.request.wrappers.BrotliRequestWrapper;
import io.github.booster.web.handler.compression.request.wrappers.CompressRequestWrapper;
import io.github.booster.web.handler.compression.request.wrappers.DeflateRequestWrapper;
import io.github.booster.web.handler.compression.request.wrappers.GZIPRequestWrapper;
import io.github.booster.web.handler.compression.response.wrappers.BrotliResponseWrapper;
import io.github.booster.web.handler.compression.response.wrappers.CompressResponseWrapper;
import io.github.booster.web.handler.compression.response.wrappers.DeflateResponseWrapper;
import io.github.booster.web.handler.compression.response.wrappers.GZipResponseWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DecompressionFilter extends OncePerRequestFilter {

    private final static Set<String> allowedEncodings =
            Set.of(
                    CompressionAlgorithm.GZIP.getAlgorithm(),
                    CompressionAlgorithm.DEFLATE.getAlgorithm(),
                    CompressionAlgorithm.BROTLI.getAlgorithm()
            );

    private final static Set<String> supportedCompressionEncodings =
            Set.of(
                    CompressionAlgorithm.DEFLATE.getAlgorithm(),
                    CompressionAlgorithm.NONE.getAlgorithm(),
                    CompressionAlgorithm.GZIP.getAlgorithm()
            );

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        HttpServletRequest req = this.handleRequest(request);
        HttpServletResponse resp = this.handleResponse(req, response);
        filterChain.doFilter(req, resp);
    }

    private HttpServletResponse handleResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String acceptEncoding = request.getHeader(HttpHeaders.ACCEPT_ENCODING);
        CompressionAlgorithm algorithm = AcceptEncodingParser.findCompressionAlgorithm(acceptEncoding, supportedCompressionEncodings);

        HttpServletResponse resp = response;
        switch (algorithm) {
            case GZIP:
                resp = new GZipResponseWrapper(resp);
                break;
            case COMPRESS:
                resp = new CompressResponseWrapper(resp);
                break;
            case DEFLATE:
                resp = new DeflateResponseWrapper(resp);
                break;
            case BROTLI:
                resp = new BrotliResponseWrapper(resp);
                break;
        }
        return resp;
    }

    private HttpServletRequest handleRequest(HttpServletRequest request) throws IOException {
        String contentEncodings = request.getHeader(HttpHeaders.CONTENT_ENCODING);

        if (StringUtils.isBlank(contentEncodings)) {
            return request;
        }

        List<String> stream = Stream.of(contentEncodings.split(","))
                .map(String::strip)
                .filter(allowedEncodings::contains)
                .collect(Collectors.toList());

        List<String> reverse = new ArrayList<>();
        for (int i = stream.size() - 1; i >= 0; i--) {
            reverse.add(stream.get(i));
        }
        stream = reverse;

        HttpServletRequest req = request;
        for (String encoding: stream) {

            CompressionAlgorithm algorithm = CompressionAlgorithm.Companion.findAlgorithm(encoding);

            switch (algorithm) {
                case GZIP:
                    req = new GZIPRequestWrapper(req);
                    break;
                case COMPRESS:
                    req = new CompressRequestWrapper(req);
                    break;
                case DEFLATE:
                    req = new DeflateRequestWrapper(req);
                    break;
                case BROTLI:
                    req = new BrotliRequestWrapper(req);
                    break;
            }
        }
        return req;
    }
}
