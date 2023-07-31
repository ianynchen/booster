package io.github.booster.messaging.publisher;

import arrow.core.Either;
import arrow.core.Option;
import reactor.core.publisher.Mono;

/**
 * Publishes a message to message queue.
 * @param <T> type of message to be published.
 */
public interface MessagePublisher<T> {

    /**
     * Publishes a message to messaging system.
     * @param topic topic to publish to
     * @param message the message to be published.
     * @return {@link PublisherRecord} when successful, or {@link Throwable} in case of exceptions.
     */
    Mono<Either<Throwable, Option<PublisherRecord>>> publish(String topic, T message);
}
