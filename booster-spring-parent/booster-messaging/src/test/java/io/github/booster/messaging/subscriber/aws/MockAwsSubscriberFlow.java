package io.github.booster.messaging.subscriber.aws;

import io.github.booster.messaging.subscriber.SubscriberFlow;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockAwsSubscriberFlow implements SubscriberFlow<Message> {

    private final String name;

    private final AtomicInteger count = new AtomicInteger(0);

    public MockAwsSubscriberFlow(
            String name
    ) {
        this.name = name;
    }

    private Message createMessage() {
        Message message = mock(Message.class);
        when(message.messageId()).thenReturn(Integer.toString(count.getAndIncrement()));
        when(message.body()).thenReturn(UUID.randomUUID().toString());
        return message;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Flux<Message> flatFlux() {
        return Flux.generate(sink -> sink.next(this.createMessage()));
    }
}

