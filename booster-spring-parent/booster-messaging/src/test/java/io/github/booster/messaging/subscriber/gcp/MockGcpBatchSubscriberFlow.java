package io.github.booster.messaging.subscriber.gcp;

import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.converter.SimplePubSubMessageConverter;
import com.google.pubsub.v1.PubsubMessage;
import io.github.booster.messaging.subscriber.BatchSubscriberFlow;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.util.concurrent.ListenableFuture;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockGcpBatchSubscriberFlow implements BatchSubscriberFlow<AcknowledgeablePubsubMessage> {

    private final String name;

    private final boolean shouldAcknowledge;

    private final AtomicInteger count = new AtomicInteger(0);

    public MockGcpBatchSubscriberFlow(
            String name,
            boolean shouldAcknowledge
    ) {
        this.name = name;
        this.shouldAcknowledge = shouldAcknowledge;
    }

    private AcknowledgeablePubsubMessage createMessage(int value) {
        AcknowledgeablePubsubMessage message = mock(AcknowledgeablePubsubMessage.class);

        ListenableFuture<Void> ackResult = this.shouldAcknowledge ?
                new AsyncResult<>(null) :
                AsyncResult.forExecutionException(new IllegalStateException("ack error"));
        when(message.ack()).thenReturn(ackResult);

        PubsubMessage pubsubMessage = new SimplePubSubMessageConverter(StandardCharsets.UTF_8)
                .toPubSubMessage(Integer.toString(value), Map.of());
        when(message.getPubsubMessage()).thenReturn(pubsubMessage);

        return message;
    }

    private List<AcknowledgeablePubsubMessage> createMessages() {
        return IntStream.range(0, this.count.incrementAndGet())
                .mapToObj(value -> this.createMessage(value))
                .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Flux<List<AcknowledgeablePubsubMessage>> flux() {
        return Flux.generate(sink -> sink.next(this.createMessages()));
    }
}
