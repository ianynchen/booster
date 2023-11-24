package io.github.booster.web.handler.compression.response.wrappers;

import io.github.booster.web.handler.compression.CompressionTestData;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class GZipResponseWrapperTest {

    @Test
    void shouldCreate() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThat(new GZipResponseWrapper(response), notNullValue());
    }

    @Test
    void shouldCompress() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        GZipResponseWrapper responseWrapper = new GZipResponseWrapper(response);
        responseWrapper.getOutputStream().write(CompressionTestData.getUtfBytes(CompressionTestData.TEXT_TO_COMPRESS));
        responseWrapper.getOutputStream().flush();
        responseWrapper.getOutputStream().close();
        String result = CompressionTestData.encode(response.getContentAsByteArray());
        assertThat(result, equalTo(CompressionTestData.GZIP_COMPRESSED));
    }
}
