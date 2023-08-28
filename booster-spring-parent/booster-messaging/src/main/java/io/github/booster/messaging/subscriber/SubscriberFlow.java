package io.github.booster.messaging.subscriber;

import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Message subscriber interface
 * @param <T> type of payload to be received by the subscriber
 */
public interface SubscriberFlow<T> {

    /**
     * Name of subscriber
     * @return name of subscriber
     */
    String getName();

    /**
     * A {@link Flux} of subscription events
     * @return A {@link Flux} of subscription events
     */
    Flux<T> flatFlux();
}
