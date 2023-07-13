package io.github.booster.messaging.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import reactor.core.publisher.Mono;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link ListenableFuture} to {@link Mono}
 */
public interface FutureHelper {

    Logger log = LoggerFactory.getLogger(FutureHelper.class);

    /**
     * Converts a {@link ListenableFuture} to {@link Mono}
     * @param future future to be converted
     * @param <T> type of object future returns
     * @return <pre>{@code Mono<T>}</pre>
     */
    static <T> Mono<T> fromListenableFutureToMono(ListenableFuture<T> future) {
        return Mono.create(sink -> {
            future.addCallback(new ListenableFutureCallback<T>() {

                @Override
                public void onFailure(Throwable throwable) {
                    sink.error(throwable);
                }

                @Override
                public void onSuccess(T t) {
                    sink.success(t);
                }
            });
        });
    }

    static boolean fromListenableFutureToBoolean(ListenableFuture<Void> future) {

        AtomicBoolean result = new AtomicBoolean(true);

        CountDownLatch latch = new CountDownLatch(1);
        future.addCallback(new ListenableFutureCallback<Void>() {
            @Override
            public void onFailure(Throwable ex) {
                result.set(false);
                latch.countDown();
            }

            @Override
            public void onSuccess(Void r) {
                result.set(true);
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error("booster-messaging - listenable future await exception", e);
            return false;
        }
        return result.get();
    }
}
