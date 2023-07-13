package io.github.booster.messaging.publisher.kafka;

import arrow.core.Either;
import com.google.common.base.Preconditions;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.util.EitherUtil;
import io.github.booster.config.thread.ThreadPoolConfig;
import io.github.booster.messaging.MessagingMetricsConstants;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.github.booster.messaging.publisher.MessagePublisher;
import io.github.booster.messaging.publisher.PublisherRecord;
import io.github.booster.messaging.util.FutureHelper;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

/**
 * Uses {@link KafkaTemplate} to publish messages to Kafka
 * @param <T> payload type.
 */
public class TemplateKafkaPublisher<T> implements MessagePublisher<KafkaRecord<T>> {

    static class TemplateTextMapSetter implements TextMapSetter<ProducerRecord> {

        @Override
        public void set(@Nullable ProducerRecord carrier, String key, String value) {
            if (carrier != null) {
                carrier.headers().add(new RecordHeader(key, value.getBytes(StandardCharsets.UTF_8)));
            }
        }
    }

    private final static Logger log = LoggerFactory.getLogger(TemplateKafkaPublisher.class);

    private final static TemplateTextMapSetter textMapSetter = new TemplateTextMapSetter();

    private final String name;

    private final KafkaTemplate<String, T> kafkaTemplate;

    private final ExecutorService executorService;

    private final MetricsRegistry registry;

    private final OpenTelemetryConfig openTelemetryConfig;

    private final boolean manuallyInjectTrace;

    /**
     * Constructs publisher
     * @param name name of publisher, name is also used to get thread pool to send
     * @param kafkaTemplate {@link KafkaTemplate} to actually send messages.
     * @param threadPoolConfig thread pool to provide threads for send. the named thread pool must be present.
     * @param registry to record metrics
     */
    public TemplateKafkaPublisher(
        String name,
        KafkaTemplate<String, T> kafkaTemplate,
        ThreadPoolConfig threadPoolConfig,
        MetricsRegistry registry,
        OpenTelemetryConfig openTelemetryConfig,
        boolean manuallyInjectTrace
    ) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank");
        Preconditions.checkArgument(kafkaTemplate != null, "kafka template cannot be null");
        Preconditions.checkArgument(threadPoolConfig != null, "thread poll config cannot be null");
        Preconditions.checkArgument(threadPoolConfig.get(name) != null, "executor service cannot be null");

        this.name = name;
        this.kafkaTemplate = kafkaTemplate;
        this.executorService = threadPoolConfig.get(name);
        this.registry = registry == null ? new MetricsRegistry() : registry;
        this.openTelemetryConfig = openTelemetryConfig;
        this.manuallyInjectTrace = manuallyInjectTrace;
    }

    private ProducerRecord<String, T> createProducerRecord(String topic, KafkaRecord<T> message) {
        return new ProducerRecord<>(topic, message.getKey(), message.getValue());
    }

    /**
     * Publishes to kafka
     * @param topic topic to publish to
     * @param message the message to be published.
     * @return {@link PublisherRecord} when successful, or {@link Throwable} in case of exceptions.
     */
    @Override
    public Mono<Either<Throwable, PublisherRecord>> publish(String topic, KafkaRecord<T> message) {
        val sample = this.registry.startSample();

        log.debug("booster-messaging - kafka publisher[{}] published to Kafka topic {}", this.name, topic);
        ProducerRecord<String, T> producerRecord = this.createProducerRecord(topic, message);

        if (this.manuallyInjectTrace && this.openTelemetryConfig != null){
            this.openTelemetryConfig.getOpenTelemetry()
                    .getPropagators()
                    .getTextMapPropagator()
                    .inject(Context.current(), producerRecord, textMapSetter);
        }

        return FutureHelper.fromListenableFutureToMono(this.kafkaTemplate.send(producerRecord))
                .subscribeOn(Schedulers.fromExecutorService(this.executorService))
                .map(sendResult -> {
                    log.debug("booster-messaging - kafka publisher[{}] published to Kafka with result: {}", this.name, sendResult);
                    this.registry.incrementCounter(
                            MessagingMetricsConstants.SEND_COUNT,
                            MessagingMetricsConstants.MESSAGING_TYPE, MessagingMetricsConstants.KAFKA,
                            MessagingMetricsConstants.NAME, this.name,
                            MessagingMetricsConstants.TOPIC, topic,
                            MessagingMetricsConstants.STATUS, MessagingMetricsConstants.SUCCESS_STATUS,
                            MessagingMetricsConstants.REASON, MessagingMetricsConstants.SUCCESS_STATUS
                    );
                    return EitherUtil.convertData(
                            PublisherRecord.builder()
                                    .topic(topic)
                                    .recordId(
                                            Long.toString(
                                                    sendResult.getRecordMetadata()
                                                            .offset()
                                            )
                                    ).build()
                    );
                }).onErrorResume(throwable -> {
                    log.error("booster-messaging - kafka publisher[{}] error publishing to kafka topic: {}", this.name, topic, throwable);
                    this.registry.incrementCounter(
                            MessagingMetricsConstants.SEND_COUNT,
                            MessagingMetricsConstants.MESSAGING_TYPE, MessagingMetricsConstants.KAFKA,
                            MessagingMetricsConstants.NAME, this.name,
                            MessagingMetricsConstants.TOPIC, topic,
                            MessagingMetricsConstants.STATUS, MessagingMetricsConstants.FAILURE_STATUS,
                            MessagingMetricsConstants.REASON, throwable.getClass().getSimpleName()
                    );
                    return Mono.just(EitherUtil.convertThrowable(throwable));
                }).doOnTerminate(() -> {
                    log.debug("booster-messaging - kafka publisher[{}] message send terminated", this.name);
                    this.registry.endSample(
                            sample,
                            MessagingMetricsConstants.SEND_TIME,
                            MessagingMetricsConstants.NAME, this.name,
                            MessagingMetricsConstants.MESSAGING_TYPE, MessagingMetricsConstants.KAFKA
                    );
                });
    }
}
