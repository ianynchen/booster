package io.github.booster.messaging.queue;

import arrow.core.Option;
import com.google.common.base.Preconditions;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.config.thread.ThreadPoolConfigGeneric;
import io.github.booster.messaging.MessagingMetricsConstants;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

/**
 * <p>Internally uses a {@link BlockingQueue} to store messages coming
 * from subscribers. A consumer then pull messages
 * from {@link BlockingQueue} to process. Consumers are
 * each run on a separate thread to maximize CPU utilization. This is helpful
 * when number of subscribers are limited.</p>
 *
 * <p>Purpose of using a {@link BlockingQueue} is to use back pressure to
 * on any message queue consumers.</p>
 *
 * @param <T> type of message to be stored.
 */
public class MessageQueue<T> {

    private static final Logger log = LoggerFactory.getLogger(MessageQueue.class);

    private final String name;

    private final BlockingQueue<T> queue;

    private final MetricsRegistry registry;

    private volatile boolean stopped;

    private final ExecutorService executorService;

    public MessageQueue(
            String name,
            ThreadPoolConfigGeneric threadPoolConfig,
            MetricsRegistry registry,
            int size
    ) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank");
        Preconditions.checkArgument(size > 0, "queue size must be greater than 0");
        Preconditions.checkArgument(threadPoolConfig != null, "thread pool config cannot be null");

        this.queue = new ArrayBlockingQueue<>(size);
        this.registry = registry == null ? new MetricsRegistry() : registry;
        this.name = name;
        this.executorService = threadPoolConfig.get(name);
    }

    public MessageQueue(
            String name,
            ThreadPoolConfigGeneric threadPoolConfig,
            MetricsRegistry registry,
            BlockingQueue<T> queue
    ) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank");
        Preconditions.checkArgument(registry != null, "MetricsRegistry cannot be null");
        Preconditions.checkArgument(threadPoolConfig != null, "thread pool config cannot be null");
        Preconditions.checkArgument(queue != null, "blocking queue cannot be null");

        this.queue = queue;
        this.registry = registry;
        this.name = name;
        this.executorService = threadPoolConfig.get(name);
    }

    /**
     * Returns the messages as a {@link Flux} for async processing.
     * @return {@link Flux} of messages
     */
    public Flux<Option<T>> flux() {
        val flux = Flux.generate(
                () -> this.stopped,
                (Boolean stopState, SynchronousSink<Option<T>> sink) -> {
                    val sampleOption = this.registry.startSample();
                    try {
                        log.debug("booster-messaging - polling message from queue[{}]", this.name);
                        T message = this.queue.take();
                        this.registry.incrementCounter(
                                MessagingMetricsConstants.DEQUEUE_COUNT,
                                MessagingMetricsConstants.NAME, this.name,
                                MessagingMetricsConstants.STATUS, MessagingMetricsConstants.SUCCESS_STATUS,
                                MessagingMetricsConstants.REASON, MessagingMetricsConstants.SUCCESS_STATUS
                        );
                        log.debug("booster-messaging - message polled from queue[{}], message: [{}]", this.name, message);
                        sink.next(Option.fromNullable(message));
                    } catch (Throwable e) {
                        log.warn("booster-messaging - error polling from queue", e);
                        this.registry.incrementCounter(
                                MessagingMetricsConstants.DEQUEUE_COUNT,
                                MessagingMetricsConstants.NAME, this.name,
                                MessagingMetricsConstants.STATUS, MessagingMetricsConstants.FAILURE_STATUS,
                                MessagingMetricsConstants.REASON, e.getClass().getSimpleName()
                        );
                        sink.next(Option.fromNullable(null));
                    }
                    this.registry.endSample(
                            sampleOption,
                            MessagingMetricsConstants.DEQUEUE_TIME,
                            MessagingMetricsConstants.NAME, this.name
                    );
                    if (this.stopped) {
                        sink.complete();
                    }
                    return this.stopped;
                },
                stopState -> {
                    if (this.executorService != null) {
                        log.info("booster-messaging - shutting down thread pool for queue[{}]", this.name);
                        this.executorService.shutdown();
                    }
                    log.info("booster-messaging - queue[{}] stopped", this.name);
                }
        );
        if (this.executorService != null) {
            log.debug("booster-messaging - running queue[{}] with thread pool", this.name);
            return flux.subscribeOn(Schedulers.fromExecutorService(this.executorService));
        }
        log.debug("booster-messaging - running queue[{}] on calling thread", this.name);
        return flux;
    }

    /**
     * Push message to {@link BlockingQueue}. This operation may be blocked.
     * @param message message to be pushed
     * @return {@link MessageQueue}
     */
    public MessageQueue<T> push(T message) {
        val sampleOption = this.registry.startSample();
        try {
            log.debug("booster-messaging - enqueuing to queue[{}], message [{}]", this.name, message);
            this.queue.put(message);
            this.registry.incrementCounter(
                    MessagingMetricsConstants.ENQUEUE_COUNT,
                    MessagingMetricsConstants.NAME, this.name,
                    MessagingMetricsConstants.STATUS, MessagingMetricsConstants.SUCCESS_STATUS,
                    MessagingMetricsConstants.REASON, MessagingMetricsConstants.SUCCESS_STATUS
            );
            log.debug("booster-messaging - message enqueued to queue[{}]: [{}]", this.name, message);
        } catch (Throwable e) {
            log.warn("booster-messaging - exception enqueuing message to queue[{}]", this.name, e);
            this.registry.incrementCounter(
                    MessagingMetricsConstants.ENQUEUE_COUNT,
                    MessagingMetricsConstants.NAME, this.name,
                    MessagingMetricsConstants.STATUS, MessagingMetricsConstants.FAILURE_STATUS,
                    MessagingMetricsConstants.REASON, e.getClass().getSimpleName()
            );
        }
        this.registry.endSample(
                sampleOption,
                MessagingMetricsConstants.ENQUEUE_TIME,
                MessagingMetricsConstants.NAME, this.name
        );
        return this;
    }

    /**
     * Stops message queue and all threads being used.
     * @return {@link MessageQueue}
     */
    public MessageQueue<T> stop() {
        log.info("booster-messaging - stopping message queue[{}]", this.name);
        this.stopped = true;
        log.info("booster-messaging - messaged queue[{}] stop signal sent", this.name);
        return this;
    }
}
