package io.github.booster.messaging.publisher.kafka;

import arrow.core.Either;
import arrow.core.Option;
import com.google.common.base.Preconditions;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.util.EitherUtil;
import io.github.booster.config.thread.ThreadPoolConfig;
import io.github.booster.messaging.MessagingMetricsConstants;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.publisher.PublisherRecord;
import io.github.booster.messaging.publisher.MessagePublisher;
import io.github.booster.messaging.util.MetricsHelper;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

/**
 * Kafka publisher using {@link KafkaSender}
 * @param <T> type of payload
 */
public class ReactiveKafkaPublisher<T> implements MessagePublisher<KafkaRecord<T>> {

    private static class ReactorTextMapSetter<T> implements TextMapSetter<SenderRecord<String, T, ?>> {

        @Override
        public void set(@Nullable SenderRecord<String, T, ?> carrier, String key, String value) {
            carrier.headers().add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private final static Logger log = LoggerFactory.getLogger(ReactiveKafkaPublisher.class);

    private final ReactorTextMapSetter<T> textMapSetter = new ReactorTextMapSetter<>();

    private final String name;

    private final KafkaSender<String, T> kafkaSender;

    private final ExecutorService executorService;

    private final MetricsRegistry registry;

    private final OpenTelemetryConfig openTelemetryConfig;

    private final boolean manuallyInjectTrace;

    /**
     * Constructs publisher
     * @param name name of publisher, name is also used to get thread pool to send
     * @param kafkaSender {@link KafkaSender} to actually send messages.
     * @param threadPoolConfig thread pool to provide threads for send. the named thread pool must be present.
     * @param registry to record metrics
     */
    public ReactiveKafkaPublisher(
        String name,
        KafkaSender<String, T> kafkaSender,
        ThreadPoolConfig threadPoolConfig,
        MetricsRegistry registry,
        OpenTelemetryConfig openTelemetryConfig,
        boolean manuallyInjectTrace
    ) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank");
        Preconditions.checkArgument(kafkaSender != null, "kafka sender cannot be null");
        Preconditions.checkArgument(threadPoolConfig != null, "thread poll config cannot be null");
        Preconditions.checkArgument(threadPoolConfig.get(name) != null, "executor service cannot be null");

        this.name = name;
        this.kafkaSender = kafkaSender;
        this.executorService = threadPoolConfig.get(name);
        this.registry = registry == null ? new MetricsRegistry() : registry;
        this.openTelemetryConfig = openTelemetryConfig;
        this.manuallyInjectTrace = manuallyInjectTrace;
    }

    private SenderRecord<String, T, String> createSenderRecord(String topic, KafkaRecord<T> record) {
        SenderRecord<String, T, String> senderRecord = SenderRecord.create(
                new ProducerRecord<>(topic, record.getKey(), record.getValue()),
                record.getCorrelationKey()
        );

        if (this.manuallyInjectTrace && this.openTelemetryConfig != null) {
            this.openTelemetryConfig.getOpenTelemetry()
                    .getPropagators()
                    .getTextMapPropagator()
                    .inject(Context.current(), senderRecord, textMapSetter);
        }

        return senderRecord;
    }

    private Mono<Either<Throwable, Option<PublisherRecord>>> convert(String topic, Flux<SenderResult<String>> senderResultFlux) {
        Mono<SenderResult<String>> senderResultMono = senderResultFlux.last();
        return senderResultMono.map(senderResult -> {
            if (senderResult.exception() != null) {
                log.error("booster-messaging - kafka publisher[{}] error publishing to kafka topic: {}", this.name, topic, senderResult.exception());
                MetricsHelper.recordMessagePublishCount(
                        this.registry,
                        MessagingMetricsConstants.SEND_COUNT,
                        MessagingMetricsConstants.KAFKA,
                        this.name,
                        topic,
                        MessagingMetricsConstants.FAILURE_STATUS,
                        senderResult.exception().getClass().getSimpleName()
                );

                return EitherUtil.convertThrowable(senderResult.exception());
            } else {
                log.debug("booster-messaging - kafka publisher[{}] published to Kafka with result: {}", this.name, senderResult);
                MetricsHelper.recordMessagePublishCount(
                        this.registry,
                        MessagingMetricsConstants.SEND_COUNT,
                        MessagingMetricsConstants.KAFKA,
                        this.name,
                        topic,
                        MessagingMetricsConstants.SUCCESS_STATUS,
                        MessagingMetricsConstants.SUCCESS_STATUS
                );

                return EitherUtil.convertData(
                        Option.fromNullable(
                                PublisherRecord.builder()
                                        .topic(topic)
                                        .recordId(
                                                Long.toString(senderResult.recordMetadata().offset())
                                        ).build()
                        )
                );
            }
        });
    }

    /**
     * Publishes to kafka
     * @param topic topic to publish to
     * @param message the message to be published.
     * @return {@link PublisherRecord} when successful, or {@link Throwable} in case of exceptions.
     */
    @Override
    public Mono<Either<Throwable, Option<PublisherRecord>>> publish(String topic, KafkaRecord<T> message) {

        val sample = this.registry.startSample();

        SenderRecord<String, T, String> senderRecord = this.createSenderRecord(topic, message);

        log.debug("booster-messaging - kafka publisher[{}] sending to topic {}", this.name, topic);

        Flux<SenderResult<String>> senderResultFlux = this.kafkaSender.send(Mono.just(senderRecord))
                .subscribeOn(Schedulers.fromExecutorService(this.executorService))
                .doOnTerminate(() -> {
                    log.debug("booster-messaging - kafka publisher[{}] message send terminated", this.name);
                    MetricsHelper.recordProcessingTime(
                            this.registry,
                            sample,
                            MessagingMetricsConstants.SEND_TIME,
                            MessagingMetricsConstants.KAFKA,
                            this.name
                    );
                });

        return this.convert(topic, senderResultFlux);
    }
}
