package io.github.booster.web.handler.compression;

import io.github.booster.commons.compression.output.DeflateOutputStream;
import io.github.booster.commons.compression.output.GZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static io.github.booster.web.handler.compression.CompressionTestData.BROTLI_COMPRESSED;
import static io.github.booster.web.handler.compression.CompressionTestData.DEFLATE_COMPRESSED;
import static io.github.booster.web.handler.compression.CompressionTestData.GZIP_COMPRESSED;
import static io.github.booster.web.handler.compression.CompressionTestData.TEXT_TO_COMPRESS;
import static io.github.booster.web.handler.compression.CompressionTestData.getUtfBytes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class CompressionFilterTest {

    public static class VerifyingFilterChain implements FilterChain {

        private final boolean verifyInput;

        private final String outputString;

        private final MockHttpServletResponse mockResponse;

        public VerifyingFilterChain(
                MockHttpServletResponse outputStream,
                boolean verifyInput,
                String outputString
        ) {
            this.verifyInput = verifyInput;
            this.outputString = outputString;
            this.mockResponse = outputStream;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            if (this.verifyInput) {
                this.verifyInput(request);
            } else {
                response.getOutputStream().write(CompressionTestData.getUtfBytes(TEXT_TO_COMPRESS));
                response.getOutputStream().flush();
                response.getOutputStream().close();;
                this.verifyOutput();
            }
        }

        private void verifyOutput() {
            byte[] bytes = this.mockResponse.getContentAsByteArray();
            assertThat(
                    new String(Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8),
                    equalTo(this.outputString)
            );
        }

        private void verifyInput(ServletRequest request) throws IOException {
            String str = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(str, equalTo(TEXT_TO_COMPRESS));
        }
    }

    private String getBase64EncodedString(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    private byte[] getUTF8Bytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] getBase64DecodedBytes(String str) {
        return Base64.getDecoder().decode(getUTF8Bytes(str));
    }

    @Test
    void shouldHandleOriginal() throws ServletException, IOException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setContent(getUTF8Bytes(TEXT_TO_COMPRESS));
        CompressionFilter filter = new CompressionFilter();
        filter.doFilter(mockRequest, new MockHttpServletResponse(), new MockFilterChain());

        byte[] bytes = mockRequest.getInputStream().readAllBytes();
        String result = new String(bytes, StandardCharsets.UTF_8);
        assertThat(result, equalTo(TEXT_TO_COMPRESS));
    }

    @Test
    void shouldUncompressGZip() throws ServletException, IOException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader(HttpHeaders.CONTENT_ENCODING, "gzip");
        mockRequest.setContent(Base64.getDecoder().decode(getUTF8Bytes(GZIP_COMPRESSED)));
        CompressionFilter filter = new CompressionFilter();
        FilterChain chain = new VerifyingFilterChain(null, true, null);
        filter.doFilter(mockRequest, new MockHttpServletResponse(), chain);
    }

    @Test
    void shouldUncompressDeflate() throws ServletException, IOException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader(HttpHeaders.CONTENT_ENCODING, "deflate");
        mockRequest.setContent(Base64.getDecoder().decode(getUTF8Bytes(DEFLATE_COMPRESSED)));
        CompressionFilter filter = new CompressionFilter();
        FilterChain chain = new VerifyingFilterChain(null, true, null);
        filter.doFilter(mockRequest, new MockHttpServletResponse(), chain);
    }

    @Test
    void shouldUncompressBrotli() throws ServletException, IOException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader(HttpHeaders.CONTENT_ENCODING, "br");
        mockRequest.setContent(Base64.getDecoder().decode(getUTF8Bytes(BROTLI_COMPRESSED)));
        CompressionFilter filter = new CompressionFilter();
        FilterChain chain = new VerifyingFilterChain(null, true, null);
        filter.doFilter(mockRequest, new MockHttpServletResponse(), chain);
    }

    @Test
    void shouldUncompressChain2() throws IOException, ServletException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZipOutputStream gZipOutputStream = new GZipOutputStream(byteArrayOutputStream);
        DeflateOutputStream deflateOutputStream = new DeflateOutputStream(gZipOutputStream);

        deflateOutputStream.write(TEXT_TO_COMPRESS.getBytes(StandardCharsets.UTF_8));
        deflateOutputStream.flush();
        deflateOutputStream.close();

        byte[] compressedContent = byteArrayOutputStream.toByteArray();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader(HttpHeaders.CONTENT_ENCODING, "deflate,gzip");
        mockRequest.setContent(compressedContent);
        CompressionFilter filter = new CompressionFilter();
        FilterChain chain = new VerifyingFilterChain(null, true, null);
        filter.doFilter(mockRequest, new MockHttpServletResponse(), chain);
    }

    @Test
    void shouldUncompressChain3() throws IOException, ServletException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        GZipOutputStream gZipOutputStream = new GZipOutputStream(byteArrayOutputStream);
        DeflateOutputStream deflateOutputStream = new DeflateOutputStream(gZipOutputStream);

        deflateOutputStream.write(CompressionTestData.decode(BROTLI_COMPRESSED));
        deflateOutputStream.flush();
        deflateOutputStream.close();

        byte[] compressedContent = byteArrayOutputStream.toByteArray();
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader(HttpHeaders.CONTENT_ENCODING, "br,deflate,gzip");
        mockRequest.setContent(compressedContent);
        CompressionFilter filter = new CompressionFilter();
        FilterChain chain = new VerifyingFilterChain(null, true, null);
        filter.doFilter(mockRequest, new MockHttpServletResponse(), chain);
    }

    @Test
    void shouldHandleOriginalResponse() throws IOException, ServletException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        CompressionFilter filter = new CompressionFilter();
        filter.doFilter(
                mockRequest,
                mockResponse,
                new VerifyingFilterChain(
                        mockResponse,
                        false,
                        CompressionTestData.encode(getUtfBytes(TEXT_TO_COMPRESS))
                )
        );
    }

    @Test
    void shouldHandleGzipResponse() throws ServletException, IOException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        CompressionFilter filter = new CompressionFilter();
        filter.doFilter(
                mockRequest,
                mockResponse,
                new VerifyingFilterChain(
                        mockResponse,
                        false,
                        GZIP_COMPRESSED
                )
        );
    }

    @Test
    void shouldHandleDeflateResponse() throws ServletException, IOException {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader(HttpHeaders.ACCEPT_ENCODING, "deflate");
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        CompressionFilter filter = new CompressionFilter();
        filter.doFilter(
                mockRequest,
                mockResponse,
                new VerifyingFilterChain(
                        mockResponse,
                        false,
                        DEFLATE_COMPRESSED
                )
        );
    }
}
