package io.github.booster.messaging.subscriber;

import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Subscriber that receives and processes messages
 * received by batch.
 * @param <T> type of message.
 */
public interface BatchSubscriberFlow<T> {

    /**
     * Gets the name of the subscriber
     * @return name of the subscriber
     */
    String getName();

    /**
     * Returns the received messages as a {@link Flux} of batches of messages
     * @return the received messages as a {@link Flux} of batches of messages
     */
    Flux<List<T>> flux();
}
