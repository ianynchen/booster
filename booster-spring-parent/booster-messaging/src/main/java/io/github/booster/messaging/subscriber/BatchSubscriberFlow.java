package io.github.booster.messaging.subscriber;

import reactor.core.publisher.Flux;

import java.util.List;

public interface BatchSubscriberFlow<T> {

    String getName();

    Flux<List<T>> flux();
}
