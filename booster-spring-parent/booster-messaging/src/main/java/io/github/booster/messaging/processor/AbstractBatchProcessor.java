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
import io.github.booster.messaging.subscriber.BatchSubscriberFlow;
import io.github.booster.messaging.util.MetricsHelper;
import io.github.booster.messaging.util.TraceHelper;
import io.github.booster.task.Task;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A processor that listens to {@link BatchSubscriberFlow} and processes a list of events
 * at a time. After each list of events is successfully processed, the list of events will be acknowledged
 * and corresponding metrics recorded.
 *
 * Events are processed in a {@link Task}
 *
 * @param <T> type of events to be processed.
 */
abstract public class AbstractBatchProcessor<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractProcessor.class);

    /**
     * Acknowledgement failure metric name.
     */
    public static final String ACK_FAILURE = "ack_failure";

    private final String type;

    /**
     * {@link BatchSubscriberFlow}
     */
    protected final BatchSubscriberFlow<T> subscriberFlow;

    /**
     * {@link Task} to process received records.
     */
    private final Task<List<T>, List<T>> processTask;

    /**
     * {@link OpenTelemetryConfig} object used to get tracing context
     */
    protected final OpenTelemetryConfig openTelemetryConfig;

    /**
     * {@link MetricsRegistry} to record metrics
     */
    protected final MetricsRegistry registry;

    /**
     * should programmatically inject trace?
     */
    protected final boolean manuallyInjectTrace;

    /**
     * Constructor
     * @param type type of {@link AbstractBatchProcessor}, either Kafka or GCP pub/sub
     * @param subscriberFlow {@link BatchSubscriberFlow} to listen to.
     * @param processTask {@link Task} used to process events.
     * @param openTelemetryConfig {@link OpenTelemetryConfig} object used to get tracing context
     * @param registry metrics recording.
     * @param manuallyInjectTrace should programmatically inject trace?
     */
    public AbstractBatchProcessor(
            String type,
            BatchSubscriberFlow<T> subscriberFlow,
            Task<List<T>, List<T>> processTask,
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
     * Creates a tracing context from pulled records
     * @param records records pulled.
     * @return tracing context. This should be non-null.
     */
    abstract protected Context createContext(List<T> records);

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
     * Acknowledges a list of records after successful processing.
     * @param records records to be acknowledged.
     * @return number of records successfully acknowledged.
     */
    abstract protected int acknowledge(List<T> records);

    private Option<BatchProcessResult<T>> tryAcknowledge(
            List<T> records,
            int totalSize
    ) {
        int size = records.size();
        int acknowledged = this.acknowledge(records);
        int unacknowledged = size - acknowledged;
        int failed = totalSize - size;

        log.debug(
                "booster-messaging - processor[{}] message from {} subscriber[{}] processed [{}] records successfully out of a total of [{}] records",
                this.getName(),
                this.type,
                this.subscriberFlow.getName(),
                size,
                totalSize
        );

        if (acknowledged > 0) {
            log.debug(
                    "booster-messaging - processor[{}] message from {} subscriber[{}] acknowledged [{}] records",
                    this.getName(),
                    this.type,
                    this.subscriberFlow.getName(),
                    acknowledged
            );
            MetricsHelper.recordMessageSubscribeCount(
                    this.registry,
                    MessagingMetricsConstants.ACKNOWLEDGEMENT_COUNT,
                    acknowledged,
                    this.type,
                    this.subscriberFlow.getName(),
                    MessagingMetricsConstants.SUCCESS_STATUS,
                    MessagingMetricsConstants.SUCCESS_STATUS
            );
        }
        if (unacknowledged > 0) {
            log.warn(
                    "booster-messaging - processor[{}] message from {} subscriber[{}] failed to acknowledge [{}] records",
                    this.getName(),
                    this.type,
                    this.subscriberFlow.getName(),
                    unacknowledged
            );
            MetricsHelper.recordMessageSubscribeCount(
                    this.registry,
                    MessagingMetricsConstants.ACKNOWLEDGEMENT_COUNT,
                    unacknowledged,
                    this.type,
                    this.subscriberFlow.getName(),
                    MessagingMetricsConstants.FAILURE_STATUS,
                    ACK_FAILURE
            );
        }

        if (size > 0) {
            MetricsHelper.recordMessageSubscribeCount(
                    this.registry,
                    MessagingMetricsConstants.SUBSCRIBER_PROCESS_COUNT,
                    size,
                    this.type,
                    this.subscriberFlow.getName(),
                    MessagingMetricsConstants.SUCCESS_STATUS,
                    MessagingMetricsConstants.SUCCESS_STATUS
            );
        }
        if (failed > 0) {
            MetricsHelper.recordMessageSubscribeCount(
                    this.registry,
                    MessagingMetricsConstants.SUBSCRIBER_PROCESS_COUNT,
                    failed,
                    this.type,
                    this.subscriberFlow.getName(),
                    MessagingMetricsConstants.FAILURE_STATUS,
                    MessagingMetricsConstants.FAILURE_STATUS
            );
        }
        return Option.fromNullable(
                new BatchProcessResult<>(
                        records,
                        acknowledged,
                        unacknowledged,
                        failed
                )
        );
    }

    private Either<Throwable, Option<BatchProcessResult<T>>> recordMetricsAndAcknowledge(
            Either<Throwable, Option<List<T>>> processedRecords,
            int totalSize
    ) {
        return EitherKt.getOrElse(
                processedRecords.map(optionRecords -> {
                    Option<BatchProcessResult<T>> result = OptionKt.getOrElse(
                            optionRecords.map(records -> this.tryAcknowledge(records, totalSize)),
                            () -> Option.fromNullable(null)
                    );
                    return EitherUtil.convertData(result);
                }),
                (t) -> {
                    log.error(
                            "booster-messaging - processor[{}] message from {} subscriber[{}] processed with error",
                            this.getName(),
                            this.type,
                            this.subscriberFlow.getName(),
                            t
                    );
                    // record all records as failed to process.
                    MetricsHelper.recordMessageSubscribeCount(
                            this.registry,
                            MessagingMetricsConstants.SUBSCRIBER_PROCESS_COUNT,
                            totalSize,
                            this.type,
                            this.subscriberFlow.getName(),
                            MessagingMetricsConstants.FAILURE_STATUS,
                            t.getClass().getSimpleName()
                    );
                    // since all records failed processing, there's no need to record
                    // acknowledged or unacknowledged.
                    return EitherUtil.convertThrowable(t);
                }
        );
    }

    /**
     * Start listening on {@link BatchSubscriberFlow}
     * @return a {@link Flux} of {@link BatchProcessResult}
     */
    public Flux<Either<Throwable, Option<BatchProcessResult<T>>>> process() {
        AtomicReference<Span> spanReference = new AtomicReference<>();
        AtomicReference<Scope> scopeReference = new AtomicReference<>();

        return this.subscriberFlow.flux()
                .flatMap(records -> {

                    if (this.manuallyInjectTrace) {
                        Span childSpan = this.createSpan(this.createContext(records));
                        Scope childScope = this.createScope(childSpan);

                        spanReference.set(childSpan);
                        scopeReference.set(childScope);
                    }

                    log.debug(
                            "booster-messaging - in processor[{}], processing start processing message: {}",
                            this.getName(),
                            records
                    );

                    int size = records.size();
                    return this.processTask.execute(records)
                            .map(either -> this.recordMetricsAndAcknowledge(either,
                                    size));
                }).map(records -> {
                    if (this.manuallyInjectTrace) {
                        TraceHelper.releaseSpanAndScope(scopeReference, spanReference);
                    }
                    return records;
                })
                .doOnTerminate(() -> {
                    if (this.manuallyInjectTrace) {
                        TraceHelper.releaseSpanAndScope(scopeReference, spanReference);
                    }
                });
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
