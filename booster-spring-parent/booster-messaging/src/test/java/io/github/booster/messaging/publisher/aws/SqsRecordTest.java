package io.github.booster.messaging.publisher.aws;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

class SqsRecordTest {

    @Test
    void shouldCreate() {
        assertThat(
                SqsRecord.createMessage(
                        null,
                        "test"
                ),
                notNullValue()
        );
        assertThat(
                SqsRecord.createMessage(
                        Map.of("test", "test"),
                        "test"
                ),
                notNullValue()
        );
        assertThat(
                SqsRecord.createMessage(
                        null,
                        null,
                        null,
                        "test"
                ),
                notNullValue()
        );
        assertThat(
                SqsRecord.createMessage(
                        null,
                        null,
                        Map.of("test", "test"),
                        "test"
                ),
                notNullValue()
        );
        assertThat(
                SqsRecord.createMessage(
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
                () -> SqsRecord.createMessage(
                        "test",
                        null,
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> SqsRecord.createMessage(
                        null,
                        "test",
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> SqsRecord.createMessage(
                        "",
                        "test",
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> SqsRecord.createMessage(
                        " ",
                        "test",
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> SqsRecord.createMessage(
                        "test",
                        "",
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> SqsRecord.createMessage(
                        "test",
                        " ",
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> SqsRecord.createMessage(
                        "test",
                        "test",
                        null,
                        null
                )
        );
    }
}
