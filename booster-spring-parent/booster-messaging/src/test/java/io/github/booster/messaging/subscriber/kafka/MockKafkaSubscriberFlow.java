package io.github.booster.messaging.subscriber.kafka;

import io.github.booster.messaging.subscriber.SubscriberFlow;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class MockKafkaSubscriberFlow implements SubscriberFlow<SubscriberRecord<Integer>> {

    private final String name;

    private final boolean shouldAcknowledge;

    private final AtomicInteger count = new AtomicInteger(0);

    public MockKafkaSubscriberFlow(String name) {
        this(name, true);
    }

    public MockKafkaSubscriberFlow(
            String name,
            boolean shouldAcknowledge
    ) {
        this.name = name;
        this.shouldAcknowledge = shouldAcknowledge;
    }

    @Override
    public String getName() {
        return this.name;
    }

    private Acknowledgment createAcknowledgment() {
        Acknowledgment ack = mock(Acknowledgment.class);

        if (this.shouldAcknowledge) {
            doNothing().when(ack).acknowledge();
        } else {
            doThrow(new IllegalStateException("error")).when(ack).acknowledge();
        }
        return ack;
    }

    @Override
    public Flux<SubscriberRecord<Integer>> flatFlux() {
        return Flux.generate(sink -> {
            sink.next(
                    new SubscriberRecord(
                            this.count.getAndIncrement(),
                            this.createAcknowledgment()
                    )
            );
        });
    }
}
