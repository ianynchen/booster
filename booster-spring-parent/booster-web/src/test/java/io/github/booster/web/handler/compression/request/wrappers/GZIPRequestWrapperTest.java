package io.github.booster.web.handler.compression.request.wrappers;

import io.github.booster.web.handler.compression.CompressionTestData;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class GZIPRequestWrapperTest {

    @Test
    void shouldCreate() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(CompressionTestData.decode(CompressionTestData.GZIP_COMPRESSED));
        assertThat(
                new GZIPRequestWrapper(request),
                notNullValue()
        );
    }

    @Test
    void shouldDecompress() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(CompressionTestData.decode(CompressionTestData.GZIP_COMPRESSED));
        HttpServletRequest httpServletRequest = new GZIPRequestWrapper(request);

        String result = new String(httpServletRequest.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(result, equalTo(CompressionTestData.TEXT_TO_COMPRESS));
    }
}
