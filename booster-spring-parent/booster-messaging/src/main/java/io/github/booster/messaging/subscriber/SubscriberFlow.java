package io.github.booster.messaging.subscriber;

import reactor.core.publisher.Flux;

import java.util.List;

public interface SubscriberFlow<T> {

    String getName();

    Flux<T> flatFlux();
}
