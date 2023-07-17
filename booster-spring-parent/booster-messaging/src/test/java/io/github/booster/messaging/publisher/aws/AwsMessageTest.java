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
                        null,
                        "test",
                        false
                ),
                notNullValue()
        );
        assertThat(
                AwsMessage.createMessage(
                        null,
                        Map.of("test", "test"),
                        "test",
                        false
                ),
                notNullValue()
        );
        assertThat(
                AwsMessage.createMessage(
                        null,
                        null,
                        null,
                        null,
                        "test",
                        false
                ),
                notNullValue()
        );
        assertThat(
                AwsMessage.createMessage(
                        null,
                        null,
                        null,
                        Map.of("test", "test"),
                        "test",
                        false
                ),
                notNullValue()
        );
        assertThat(
                AwsMessage.createMessage(
                        null,
                        "test",
                        "test",
                        Map.of("test", "test"),
                        "test",
                        false
                ),
                notNullValue()
        );
    }

    @Test
    void shouldFailCreate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> AwsMessage.createMessage(
                        null,
                        "test",
                        null,
                        null,
                        "test",
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> AwsMessage.createMessage(
                        null,
                        null,
                        "test",
                        null,
                        "test",
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> AwsMessage.createMessage(
                        null,
                        "",
                        "test",
                        null,
                        "test",
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> AwsMessage.createMessage(
                        null,
                        " ",
                        "test",
                        null,
                        "test",
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> AwsMessage.createMessage(
                        null,
                        "test",
                        "",
                        null,
                        "test",
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> AwsMessage.createMessage(
                        null,
                        "test",
                        " ",
                        null,
                        "test",
                        false
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> AwsMessage.createMessage(
                        null,
                        "test",
                        "test",
                        null,
                        null,
                        false
                )
        );
    }

    @Test
    void shouldCreateMessage() {
        assertThat(
                AwsMessage.createMessage(
                        null,
                        "test",
                        "test",
                        Map.of("test", "test"),
                        "test",
                        false
                ).toMessage(),
                notNullValue()
        );
    }
}
