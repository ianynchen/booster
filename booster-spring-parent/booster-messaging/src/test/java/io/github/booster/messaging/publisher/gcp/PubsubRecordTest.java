package io.github.booster.messaging.publisher.gcp;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PubsubRecordTest {

    @Test
    void shouldFailCreate() {
        assertThrows(
                NullPointerException.class,
                () -> new PubsubRecord<Integer>(null)
        );
        assertThrows(
                NullPointerException.class,
                () -> new PubsubRecord<Integer>(null, Map.of())
        );
        assertThrows(
                NullPointerException.class,
                () -> new PubsubRecord<Integer>(1, null)
        );
    }

    @Test
    void shouldCreate() {
        assertThat(
                new PubsubRecord<>(1),
                notNullValue()
        );
        assertThat(
                new PubsubRecord<>(1, Map.of()),
                notNullValue()
        );
    }
}
