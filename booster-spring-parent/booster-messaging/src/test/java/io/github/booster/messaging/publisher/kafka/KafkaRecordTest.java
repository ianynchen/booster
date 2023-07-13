package io.github.booster.messaging.publisher.kafka;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KafkaRecordTest {

    @Test
    void shouldFailCreate() {
        assertThrows(
                NullPointerException.class,
                () -> new KafkaRecord<>(null, "abc")
        );
        assertThrows(
                NullPointerException.class,
                () -> new KafkaRecord<String>("abc", null)
        );
        assertThrows(
                NullPointerException.class,
                () -> new KafkaRecord<String>(null, null)
        );
        assertThrows(
                NullPointerException.class,
                () -> new KafkaRecord<>("abc", "abc", null)
        );
        assertThrows(
                NullPointerException.class,
                () -> new KafkaRecord<String>("abc", null, "abc")
        );
        assertThrows(
                NullPointerException.class,
                () -> new KafkaRecord<String>("abc", null, null)
        );
        assertThrows(
                NullPointerException.class,
                () -> new KafkaRecord<>(null, "abc", null)
        );
        assertThrows(
                NullPointerException.class,
                () -> new KafkaRecord<>(null, "abc", "abc")
        );
        assertThrows(
                NullPointerException.class,
                () -> new KafkaRecord<String>(null, null, "abc")
        );
        assertThrows(
                NullPointerException.class,
                () -> new KafkaRecord<>(null, "abc", "abc")
        );
        assertThrows(
                NullPointerException.class,
                () -> new KafkaRecord<>("abc", null, "abc")
        );
        assertThrows(
                NullPointerException.class,
                () -> new KafkaRecord<String>(null, null, "abc")
        );
        assertThrows(
                NullPointerException.class,
                () -> new KafkaRecord<String>(null, null, null)
        );
    }

    @Test
    void shouldCreate() {
        assertThat(
                new KafkaRecord<>("abc", "abc"),
                notNullValue()
        );
        assertThat(
                new KafkaRecord<>("abc", "abc", "abc"),
                notNullValue()
        );
    }
}
