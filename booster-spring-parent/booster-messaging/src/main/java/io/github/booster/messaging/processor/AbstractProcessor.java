package io.github.booster.messaging.processor;

import arrow.core.Either;
import com.google.common.base.Preconditions;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.util.EitherUtil;
import io.github.booster.messaging.MessagingMetricsConstants;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.subscriber.SubscriberFlow;
import io.github.booster.messaging.util.TraceHelper;
import io.github.booster.task.Task;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A processor that listens to {@link SubscriberFlow} and processes one event
 * at a time. After each event is successfully processed, the event will be acknowledged
 * and corresponding metrics recorded.
 *
 * Events are processed in a {@link Task}
 *
 * @param <T> type of event to be processed.
 */
abstract public class AbstractProcessor<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractProcessor.class);
    public static final String ACK_FAILURE = "ack_failure";

    private final String type;

    protected final OpenTelemetryConfig openTelemetryConfig;

    private final SubscriberFlow<T> subscriberFlow;

    private final Task<T, T> processTask;

    protected final MetricsRegistry registry;

    protected final boolean manuallyInjectTrace;

    /**
     * Constructor
     * @param type type name for processor, either Kafka or GCP pub/sub
     * @param subscriberFlow the {@link SubscriberFlow} to listen to
     * @param processTask processor {@link Task} to process events coming from {@link SubscriberFlow}
     * @param registry metrics recording.
     */
    public AbstractProcessor(
            String type,
            SubscriberFlow<T> subscriberFlow,
            Task<T, T> processTask,
            OpenTelemetryConfig openTelemetryConfig,
            MetricsRegistry registry,
            boolean manuallyInjectTrace
    ) {
        Preconditions.checkArgument(StringUtils.isNotBlank(type), "type cannot be blank");
        Preconditions.checkArgument(subscriberFlow != null, "subscriber flow cannot be null");
        Preconditions.checkArgument(processTask != null, "process task cannot be null");

        this.subscriberFlow = subscriberFlow;
        this.registry = registry == null ? new MetricsRegistry() : registry;
        this.processTask = processTask;
        this.type = type;
        this.openTelemetryConfig = openTelemetryConfig;
        this.manuallyInjectTrace = manuallyInjectTrace;
    }

    /**
     * Acknowledges a record after successful processing.
     * @param record record to be acknowledged.
     */
    abstract protected boolean acknowledge(T record);

    abstract protected Context createContext(T record);

    protected Span createSpan(Context context) {
        return TraceHelper.createSpan(this.openTelemetryConfig, context);
    }

    protected Scope createScope(Span childSpan) {
        return childSpan.makeCurrent();
    }

    /**
     * Start listening to {@link SubscriberFlow}
     */
    public Flux<Either<Throwable, ProcessResult<T>>> process() {
        AtomicReference<Span> spanReference = new AtomicReference<>();
        AtomicReference<Scope> scopeReference = new AtomicReference<>();

        return this.subscriberFlow.flatFlux()
                .flatMap(record -> {
                    if (this.manuallyInjectTrace) {
                        Span childSpan = this.createSpan(this.createContext(record));
                        Scope childScope = this.createScope(childSpan);

                        spanReference.set(childSpan);
                        scopeReference.set(childScope);
                    }
                    log.debug(
                            "booster-messaging - from subscriber[{}], processing start processing message: {}",
                            this.subscriberFlow.getName(),
                            record
                    );
                    return this.processTask.execute(record);
                }).map(this::recordMetrics)
                .map(record -> {
                    if (this.manuallyInjectTrace) {
                        TraceHelper.releaseSpanAndScope(scopeReference, spanReference);
                    }
                    return record;
                })
                .doOnTerminate(() -> {
                    if (this.manuallyInjectTrace) {
                        TraceHelper.releaseSpanAndScope(scopeReference, spanReference);
                    }
                });
    }

    @NotNull
    private Either<Throwable, ProcessResult<T>> recordMetrics(Either<Throwable, T> either) {
        if (either.isRight()) {
            Either<Throwable, ProcessResult<T>> processResult = null;
            if (this.acknowledge(either.getOrNull())) {
                this.registry.incrementCounter(
                        MessagingMetricsConstants.ACKNOWLEDGEMENT_COUNT,
                        MessagingMetricsConstants.NAME, this.subscriberFlow.getName(),
                        MessagingMetricsConstants.MESSAGING_TYPE, this.type,
                        MessagingMetricsConstants.STATUS, MessagingMetricsConstants.SUCCESS_STATUS,
                        MessagingMetricsConstants.REASON, MessagingMetricsConstants.SUCCESS_STATUS
                );
                log.debug("booster messaging - ack result: true, message: [{}]", either.getOrNull());
                processResult = EitherUtil.convertData(new ProcessResult<>(either.getOrNull(), true));
            } else {
                this.registry.incrementCounter(
                        MessagingMetricsConstants.ACKNOWLEDGEMENT_COUNT,
                        MessagingMetricsConstants.NAME, this.subscriberFlow.getName(),
                        MessagingMetricsConstants.MESSAGING_TYPE, this.type,
                        MessagingMetricsConstants.STATUS, MessagingMetricsConstants.FAILURE_STATUS,
                        MessagingMetricsConstants.REASON, ACK_FAILURE
                );
                log.warn("booster messaging - ack result: false, message: [{}]", either.getOrNull());
                processResult = EitherUtil.convertData(new ProcessResult<>(either.getOrNull(), false));
            }
            log.debug(
                    "booster-messaging - message from {} subscriber[{}] processed successfully: {}",
                    this.type,
                    this.subscriberFlow.getName(),
                    either.getOrNull()
            );
            this.registry.incrementCounter(
                    MessagingMetricsConstants.SUBSCRIBER_PROCESS_COUNT,
                    MessagingMetricsConstants.NAME, this.subscriberFlow.getName(),
                    MessagingMetricsConstants.MESSAGING_TYPE, this.type,
                    MessagingMetricsConstants.STATUS, MessagingMetricsConstants.SUCCESS_STATUS,
                    MessagingMetricsConstants.REASON, MessagingMetricsConstants.SUCCESS_STATUS
            );
            return processResult;
        } else {
            Throwable t = either.swap().getOrNull();
            log.error(
                    "booster-messaging - message from {} subscriber[{}] processed with error",
                    this.type,
                    this.subscriberFlow.getName(),
                    t
            );
            this.registry.incrementCounter(
                    MessagingMetricsConstants.SUBSCRIBER_PROCESS_COUNT,
                    MessagingMetricsConstants.NAME, this.subscriberFlow.getName(),
                    MessagingMetricsConstants.MESSAGING_TYPE, this.type,
                    MessagingMetricsConstants.STATUS, MessagingMetricsConstants.FAILURE_STATUS,
                    MessagingMetricsConstants.REASON, t.getClass().getSimpleName()
            );
            return EitherUtil.convertThrowable(t);
        }
    }

    protected String getName() {
        return this.subscriberFlow.getName();
    }
}
