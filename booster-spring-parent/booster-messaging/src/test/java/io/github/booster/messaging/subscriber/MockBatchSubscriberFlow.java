package io.github.booster.messaging.subscriber;

import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MockBatchSubscriberFlow implements BatchSubscriberFlow<Integer> {

    private final String name;

    private final AtomicInteger count = new AtomicInteger(0);

    public MockBatchSubscriberFlow(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Flux<List<Integer>> flux() {
        return Flux.generate(sink -> {
            int size = this.count.incrementAndGet();

            List<Integer> result = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                result.add(i);
            }
            sink.next(result);
        });
    }
}
