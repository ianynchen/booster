package io.github.booster.messaging.subscriber.aws;

import io.github.booster.messaging.subscriber.BatchSubscriberFlow;
import reactor.core.publisher.Flux;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockAwsBatchSubscriberFlow implements BatchSubscriberFlow<Message> {

    private final String name;

    private final AtomicInteger count = new AtomicInteger(0);

    public MockAwsBatchSubscriberFlow(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    private Message createMessage(int i) {
        Message message = mock(Message.class);
        when(message.messageId()).thenReturn(Integer.toString(i));
        when(message.body()).thenReturn(Integer.toString(i));
        return message;
    }

    @Override
    public Flux<List<Message>> flux() {
        return Flux.generate(sink -> {
            int size = this.count.incrementAndGet();

            List<Message> result = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                result.add(this.createMessage(i));
            }
            sink.next(result);
        });
    }
}
