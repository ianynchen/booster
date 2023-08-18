package io.github.booster.messaging.queue;

import arrow.core.Option;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.config.thread.ThreadPoolConfig;
import io.github.booster.config.thread.ThreadPoolSetting;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

class MessageQueueTest {
    private static final Logger log = LoggerFactory.getLogger(MessageQueueTest.class);

    public static final int SIZE = 10;

    @Test
    void shouldFailCreate() {
        ThreadPoolSetting setting = new ThreadPoolSetting();
        ThreadPoolConfig config = new ThreadPoolConfig(null, null);
        config.setSettings(Map.of("test", setting));

        assertThrows(
                IllegalArgumentException.class,
                () -> new MessageQueue(null, new ThreadPoolConfig(null, null), new MetricsRegistry(), 2)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new MessageQueue("", new ThreadPoolConfig(null, null), new MetricsRegistry(), 2)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new MessageQueue(" ", new ThreadPoolConfig(null, null), new MetricsRegistry(), 2)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new MessageQueue("abc", null, new MetricsRegistry(), 2)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new MessageQueue("abc", config, new MetricsRegistry(), 0)
        );
    }

    private void generateMessages(MessageQueue<Integer> queue) {
        Thread thread = new Thread() {

            private AtomicInteger counter = new AtomicInteger(0);

            @Override
            public void run() {
                while (counter.get() < SIZE) {
                    try {
                        Thread.sleep(1L);
                    } catch (InterruptedException e) {
                        log.warn("exception inside generate thread", e);
                        throw new RuntimeException(e);
                    }
                    int value = counter.getAndIncrement();
                    queue.push(value);
                    log.info("value pushed inside generate thread: {}", value);
                }
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    log.warn("exception inside generate thread", e);
                    throw new RuntimeException(e);
                }
                queue.stop();
            }
        };
        thread.start();
    }

    @Test
    void shouldCreate() {
        assertThat(
                new MessageQueue<Integer>("abc", new ThreadPoolConfig(null, null), new MetricsRegistry(), 2),
                notNullValue()
        );
        assertThat(
                new MessageQueue<Integer>("abc", new ThreadPoolConfig(null, null), null, 2),
                notNullValue()
        );
    }

    private void verifyExecution(MessageQueue<Integer> queue) {
        this.generateMessages(queue);
        StepVerifier.create(queue.flux().take(5).collectList())
                .consumeNextWith(values -> {
                    assertThat(
                            values,
                            containsInAnyOrder(
                                        Option.fromNullable(0),
                                        Option.fromNullable(1),
                                        Option.fromNullable(2),
                                        Option.fromNullable(3),
                                        Option.fromNullable(4)
                            )
                    );
                }).expectComplete()
                .verify();
    }

    @Test
    void shouldExecute() {
        ThreadPoolSetting setting = new ThreadPoolSetting();
        ThreadPoolConfig config = new ThreadPoolConfig(null, null);
        config.setSettings(Map.of("test", setting));
        MessageQueue<Integer> queue = new MessageQueue<>(
                "test",
                config,
                new MetricsRegistry(new SimpleMeterRegistry()),
                5
        );
        assertThat(queue, notNullValue());
        this.verifyExecution(queue);
    }

    @Test
    void shouldExecuteWithoutThreadPool() {
        ThreadPoolConfig config = new ThreadPoolConfig(null, null);
        MessageQueue<Integer> queue = new MessageQueue<>(
                "test",
                config,
                new MetricsRegistry(new SimpleMeterRegistry()),
                5
        );
        assertThat(queue, notNullValue());
        this.verifyExecution(queue);
    }

    @Test
    void shouldExecuteAndHandleTakeError() throws InterruptedException {
        ThreadPoolSetting setting = new ThreadPoolSetting();
        ThreadPoolConfig config = new ThreadPoolConfig(null, null);
        config.setSettings(Map.of("test", setting));

        BlockingQueue<Integer> blockingQueue = createMockQueue();
        MessageQueue<Integer> queue = new MessageQueue<>(
                "test",
                config,
                new MetricsRegistry(new SimpleMeterRegistry()),
                blockingQueue
        );

        for (int i = 0; i < 10; i++) {
            assertThat(queue.push(i), equalTo(queue));
        }

        StepVerifier.create(queue.flux().take(10).collectList())
                .consumeNextWith(values -> {
                    assertThat(
                            values,
                            containsInAnyOrder(
                                    Option.fromNullable(0),
                                    Option.fromNullable(1),
                                    Option.fromNullable(2),
                                    Option.fromNullable(null),
                                    Option.fromNullable(4),
                                    Option.fromNullable(5),
                                    Option.fromNullable(6),
                                    Option.fromNullable(7),
                                    Option.fromNullable(8),
                                    Option.fromNullable(9)
                            )
                    );
                }).expectComplete()
                .verify();
    }

    @Test
    void shouldHandlePutError() throws InterruptedException {
        ThreadPoolSetting setting = new ThreadPoolSetting();
        ThreadPoolConfig config = new ThreadPoolConfig(null, null);
        config.setSettings(Map.of("test", setting));

        BlockingQueue<Integer> blockingQueue = createExceptionQueue();
        MessageQueue<Integer> queue = new MessageQueue<>(
                "test",
                config,
                new MetricsRegistry(new SimpleMeterRegistry()),
                blockingQueue
        );

        for (int i = 0; i < 10; i++) {
            assertThat(queue.push(i), equalTo(queue));
        }
    }

    private AtomicInteger itemCount = new AtomicInteger(0);

    /**
     * Create a queue that allows put, and throws exception when count is at 3, otherwise
     * return count starting from 0.
     * @return
     * @throws InterruptedException
     */
    private BlockingQueue<Integer> createMockQueue() throws InterruptedException {
        BlockingQueue<Integer> queue = mock(BlockingQueue.class);

        doAnswer(invocation -> null)
                .when(queue)
                .put(anyInt());

        doAnswer(invocation -> {
            if (itemCount.get() == 3) {
                itemCount.getAndIncrement();
                throw new IllegalStateException("error");
            }
            return itemCount.getAndIncrement();
        }).when(queue).take();

        return queue;
    }

    /**
     * Returns a queue that throws exception on put, doesn't handle take.
     * @return
     * @throws InterruptedException
     */
    private BlockingQueue<Integer> createExceptionQueue() throws InterruptedException {
        BlockingQueue<Integer> queue = mock(BlockingQueue.class);

        doThrow(new InterruptedException("error"))
                .when(queue)
                .put(ArgumentMatchers.any(Integer.class));

        return queue;
    }
}
