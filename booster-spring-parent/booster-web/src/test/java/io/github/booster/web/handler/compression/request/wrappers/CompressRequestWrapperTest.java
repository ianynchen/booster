package io.github.booster.web.handler.compression.request.wrappers;

import io.github.booster.web.handler.compression.CompressionTestData;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CompressRequestWrapperTest {

    @Test
    void shouldFail() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(CompressionTestData.decode(CompressionTestData.DEFLATE_COMPRESSED));
        assertThrows(
                IOException.class,
                () -> new CompressRequestWrapper(request)
        );
    }
}
