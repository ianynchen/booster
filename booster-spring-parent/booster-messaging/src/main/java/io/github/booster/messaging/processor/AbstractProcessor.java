package io.github.booster.messaging.processor;

import arrow.core.Either;
import arrow.core.EitherKt;
import arrow.core.Option;
import arrow.core.OptionKt;
import com.google.common.base.Preconditions;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.util.EitherUtil;
import io.github.booster.messaging.MessagingMetricsConstants;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.subscriber.SubscriberFlow;
import io.github.booster.messaging.util.MetricsHelper;
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

    /**
     * acknowledgement failure metric
     */
    public static final String ACK_FAILURE = "ack_failure";

    private final String type;

    /**
     * {@link OpenTelemetryConfig} for tracing
     */
    protected final OpenTelemetryConfig openTelemetryConfig;

    /**
     * {@link SubscriberFlow} to listen to
     */
    protected final SubscriberFlow<T> subscriberFlow;

    /**
     * processor {@link Task} to process events coming from {@link SubscriberFlow}
     */
    private final Task<T, T> processTask;

    /**
     * metrics recording.
     */
    protected final MetricsRegistry registry;

    /**
     * whether to inject trace manually or rely on OTEl instrumentation
     */
    protected final boolean manuallyInjectTrace;

    /**
     * Constructor
     * @param type type name for processor, either Kafka or GCP pub/sub
     * @param subscriberFlow the {@link SubscriberFlow} to listen to
     * @param processTask processor {@link Task} to process events coming from {@link SubscriberFlow}
     * @param openTelemetryConfig {@link OpenTelemetryConfig} for tracing
     * @param registry metrics recording.
     * @param manuallyInjectTrace whether to inject trace manually or rely on OTEl instrumentation
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
     * @return true if record acknowledged, false otherwise
     */
    abstract protected boolean acknowledge(T record);

    /**
     * Creates a context from record
     * @param record record to process
     * @return {@link Context}
     */
    abstract protected Context createContext(T record);

    /**
     * Creates a {@link Span} from {@link Context}
     * @param context {@link Context} to be used to create {@link Span}
     * @return {@link Span}
     */
    protected Span createSpan(Context context) {
        return TraceHelper.createSpan(this.openTelemetryConfig, context);
    }

    /**
     * Creates a {@link Scope} for the {@link Span}
     * @param childSpan {@link Span} to be used as current span
     * @return {@link Scope} of the current span.
     */
    protected Scope createScope(Span childSpan) {
        return childSpan.makeCurrent();
    }

    /**
     * Start listening to {@link SubscriberFlow}
     * @return a {@link Flux} of {@link Either} {@link Throwable} or {@link Option} of
     *         {@link ProcessResult}. If processing fails, {@link Either.Left} is returned
     *         otherwise, {@link Option} of {@link ProcessResult} is returned
     */
    public Flux<Either<Throwable, Option<ProcessResult<T>>>> process() {
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

    private Option<ProcessResult<T>> tryAcknowledge(T record) {
        Option<ProcessResult<T>> processResultOption;
        if (this.acknowledge(record)) {
            MetricsHelper.recordMessageSubscribeCount(
                    this.registry,
                    MessagingMetricsConstants.ACKNOWLEDGEMENT_COUNT,
                    1,
                    this.type,
                    this.subscriberFlow.getName(),
                    MessagingMetricsConstants.SUCCESS_STATUS,
                    MessagingMetricsConstants.SUCCESS_STATUS
            );
            log.debug("booster-messaging - ack result: true, message: [{}]", record);
            processResultOption =
                    Option.fromNullable(
                            new ProcessResult<>(record, true)
                    );
        } else {
            MetricsHelper.recordMessageSubscribeCount(
                    this.registry,
                    MessagingMetricsConstants.ACKNOWLEDGEMENT_COUNT,
                    1,
                    this.type,
                    this.subscriberFlow.getName(),
                    MessagingMetricsConstants.FAILURE_STATUS,
                    ACK_FAILURE
            );
            log.warn("booster-messaging - ack result: false, message: [{}]", record);
            processResultOption = Option.fromNullable(
                    new ProcessResult<>(record, false)
            );
        }
        log.debug(
                "booster-messaging - message from {} subscriber[{}] processed successfully: {}",
                this.type,
                this.subscriberFlow.getName(),
                record
        );
        MetricsHelper.recordMessageSubscribeCount(
                this.registry,
                MessagingMetricsConstants.SUBSCRIBER_PROCESS_COUNT,
                1,
                this.type,
                this.subscriberFlow.getName(),
                MessagingMetricsConstants.SUCCESS_STATUS,
                MessagingMetricsConstants.SUCCESS_STATUS
        );
        return processResultOption;
    }

    @NotNull
    private Either<Throwable, Option<ProcessResult<T>>> recordMetrics(Either<Throwable, Option<T>> either) {
        return EitherKt.getOrElse(
                either.map(optionRecord -> {
                    Option<ProcessResult<T>> result = OptionKt.getOrElse(
                            optionRecord.map(this::tryAcknowledge),
                            () -> Option.fromNullable(null)
                    );
                    return EitherUtil.convertData(result);
                }),
                (t) -> {
                    log.error(
                            "booster-messaging - message from {} subscriber[{}] processed with error",
                            this.type,
                            this.subscriberFlow.getName(),
                            t
                    );
                    MetricsHelper.recordMessageSubscribeCount(
                            this.registry,
                            MessagingMetricsConstants.SUBSCRIBER_PROCESS_COUNT,
                            1,
                            this.type,
                            this.subscriberFlow.getName(),
                            MessagingMetricsConstants.FAILURE_STATUS,
                            t.getClass().getSimpleName()
                    );

                    return EitherUtil.convertThrowable(t);
                }
        );
    }

    /**
     * Gets name of processor. The name is used to get thread pool for
     * the processor.
     * @return name of processor
     */
    protected String getName() {
        return this.subscriberFlow.getName();
    }
}
