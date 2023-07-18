package io.github.booster.messaging.publisher.aws;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

class AwsMessageTest {

    @Test
    void shouldCreate() {
        assertThat(
                AwsMessage.createMessage(
                        null,
                        "test"
                ),
                notNullValue()
        );
        assertThat(
                AwsMessage.createMessage(
                        Map.of("test", "test"),
                        "test"
                ),
                notNullValue()
        );
        assertThat(
                AwsMessage.createMessage(
                        null,
                        null,
                        null,
                        "test"
                ),
                notNullValue()
        );
        assertThat(
                AwsMessage.createMessage(
                        null,
                        null,
                        Map.of("test", "test"),
                        "test"
                ),
                notNullValue()
        );
        assertThat(
                AwsMessage.createMessage(
                        "test",
                        "test",
                        Map.of("test", "test"),
                        "test"
                ),
                notNullValue()
        );
    }

    @Test
    void shouldFailCreate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AwsMessage.createMessage(
                        "test",
                        null,
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> AwsMessage.createMessage(
                        null,
                        "test",
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> AwsMessage.createMessage(
                        "",
                        "test",
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> AwsMessage.createMessage(
                        " ",
                        "test",
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> AwsMessage.createMessage(
                        "test",
                        "",
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> AwsMessage.createMessage(
                        "test",
                        " ",
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> AwsMessage.createMessage(
                        "test",
                        "test",
                        null,
                        null
                )
        );
    }
}
