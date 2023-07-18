package io.github.booster.messaging.subscriber.aws;

import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.subscriber.BatchSubscriberFlow;
import io.github.booster.messaging.subscriber.SubscriberFlow;
import io.github.booster.messaging.util.TraceHelper;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SynchronousSink;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.concurrent.ExecutorService;

public class AwsSqsSubscriber implements SubscriberFlow<Message>, BatchSubscriberFlow<Message> {

    public static class AwsSqsTextMapGetter implements TextMapGetter<Message> {

        @Override
        public Iterable<String> keys(Message carrier) {
            return carrier.messageAttributes().keySet();
        }

        @Nullable
        @Override
        public String get(@Nullable Message carrier, String key) {
            return carrier == null ? null :
                    carrier.messageAttributes().get(key).stringValue();
        }
    }

    private static final Logger log = LoggerFactory.getLogger(AwsSqsSubscriber.class);

    public static final AwsSqsTextMapGetter GETTER = new AwsSqsTextMapGetter();

    private boolean manuallyInjectTrace;

    private volatile boolean stopped = false;

    private String name;

    private ExecutorService executorService;

    private OpenTelemetryConfig openTelemetryConfig;

    private String queryUrl;

    private List<Message> pullRecords() {
        // TODO
        return List.of();
    }

    private Flux<List<Message>> generateFlux() {
        return Flux.generate(
                () -> this.stopped,
                (Boolean stopState, SynchronousSink<List<Message>> sink) -> {
                    List<Message> records = this.pullRecords();
                    sink.next(records);
                    if (this.stopped) {
                        sink.complete();
                    }
                    return this.stopped;
                },
                stopState -> {
                    if (this.executorService != null) {
                        log.info("booster-messaging - shutting down thread pool for subscriber[{}]", this.name);
                        this.executorService.shutdown();
                    }
                    log.info("booster-messaging - queue[{}] stopped", this.name);
                }
        ).filter(entry -> entry != null && !CollectionUtils.isEmpty(entry));
    }

    public void stop() {
        this.stopped = true;
        this.executorService.shutdown();
    }

    @Override
    public Flux<List<Message>> flux() {
        return this.generateFlux()
                .flatMap(records -> {
                    if (this.manuallyInjectTrace) {
                        return TraceHelper.generateContextForList(this.openTelemetryConfig, records, GETTER);
                    }
                    return Mono.just(records);
                });
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Flux<Message> flatFlux() {
        return this.generateFlux()
                .flatMap(Flux::fromIterable)
                .flatMap(record -> {
                    if (this.manuallyInjectTrace) {
                        return TraceHelper.generateContextForItem(this.openTelemetryConfig, record, GETTER);
                    }
                    return Mono.just(record);
                });
    }

    public String getQueryUrl() {
        return this.queryUrl;
    }
}
