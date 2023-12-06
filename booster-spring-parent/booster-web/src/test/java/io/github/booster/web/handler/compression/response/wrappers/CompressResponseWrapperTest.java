package io.github.booster.web.handler.compression.response.wrappers;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.Assert.assertThrows;

class CompressResponseWrapperTest {

    @Test
    void shouldFail() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThrows(
                IOException.class,
                () -> new CompressResponseWrapper(response)
        );
    }
}
