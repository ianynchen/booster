package io.github.booster.messaging.subscriber.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class SubscriberRecordTest {

    @Test
    void shouldFail() {
        assertThrows(
                NullPointerException.class,
                () -> new SubscriberRecord<Integer>(null, mock(Acknowledgment.class))
        );
        assertThrows(
                NullPointerException.class,
                () -> new SubscriberRecord<Integer>(1, null)
        );
        assertThrows(
                NullPointerException.class,
                () -> new SubscriberRecord<Integer>(null, null)
        );
    }

    @Test
    void shouldCreate() {
        assertThat(
                new SubscriberRecord<>(1, mock(Acknowledgment.class)),
                notNullValue()
        );
    }
}
