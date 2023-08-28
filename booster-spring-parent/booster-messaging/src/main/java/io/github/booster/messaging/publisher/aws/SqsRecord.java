package io.github.booster.messaging.publisher.aws;

import arrow.core.Either;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import io.github.booster.commons.util.EitherUtil;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AWS SQS record to be sent
 * @param <T> type of payload
 */
@Getter
@EqualsAndHashCode
@ToString
public class SqsRecord<T> {

    /**
     * {@link TextMapSetter} to inject trace
     */
    public static class SQSTextMapperSetter implements TextMapSetter<Map<String, String>> {

        /**
         * Default constructor
         */
        public SQSTextMapperSetter() {
            super();
        }

        /**
         * Sets trace to carrier
         * @param carrier holds propagation fields. For example, an outgoing message or http request. To
         *     facilitate implementations as java lambdas, this parameter may be null.
         * @param key the key of the field.
         * @param value the value of the field.
         */
        @Override
        public void set(@Nullable Map<String, String> carrier, String key, String value) {
            if (carrier != null && key != null) {
                carrier.put(key, value);
            }
        }
    }

    private final static SQSTextMapperSetter SETTER = new SQSTextMapperSetter();

    private final Map<String, String> headers;

    private final String groupId;

    private final String deduplicationId;

    private final Map<String, String> traces;

    private final T body;

    /**
     * Constructs a {@link SqsRecord}
     * @param headers headers to be sent
     * @param body payload to be sent
     */
    protected SqsRecord(
            Map<String, String> headers,
            T body
    ) {
        this(null, null, headers, body);
    }

    /**
     * Constructs a {@link SqsRecord}
     * @param groupId group ID to be used
     * @param deduplicationId deduplication ID
     * @param headers headers
     * @param body payload to be sent
     */
    protected SqsRecord(
            String groupId,
            String deduplicationId,
            Map<String, String> headers,
            T body
    ) {
        if (groupId == null || deduplicationId == null) {
            Preconditions.checkArgument(
                    groupId == null && deduplicationId == null,
                    "group id and deduplication id need to be both present or both omitted"
            );
        } else {
            Preconditions.checkArgument(StringUtils.isNotBlank(groupId), "group id cannot be blank");
            Preconditions.checkArgument(StringUtils.isNotBlank(deduplicationId), "deduplication id cannot be blank");
        }
        Preconditions.checkArgument(body != null, "body cannot be null");

        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>(2);
        this.groupId = groupId;
        this.deduplicationId = deduplicationId;
        this.body = body;
        this.traces = new HashMap<>();
    }

    /**
     * Creates a {@link SqsRecord} object
     * @param headers headers
     * @param body payload
     * @return {@link SqsRecord}
     * @param <T> type of payload
     */
    public static <T> SqsRecord<T> createMessage(
            Map<String, String> headers,
            T body
    ) {
        return createMessage(
                null,
                null,
                headers,
                body
        );
    }

    /**
     * Creates a {@link SqsRecord} object
     * @param groupId group ID to be used
     * @param deduplicationId deduplication ID
     * @param headers headers
     * @param body payload
     * @return {@link SqsRecord}
     * @param <T> type of payload
     */
    public static <T> SqsRecord<T> createMessage(
            String groupId,
            String deduplicationId,
            Map<String, String> headers,
            T body
    ) {
        return new SqsRecord<>(groupId, deduplicationId, headers, body);
    }

    /**
     * Creates {@link SendMessageRequest} to be sent to SQS
     * @param queueUrl queue URL to bet sent to
     * @param mapper used to serialize the payload
     * @param openTelemetryConfig used to inject trace
     * @param manuallyInjectTrace whether to inject trace or leave it to OTEL instrumentation
     * @return {@link Either} {@link Throwable} or {@link SendMessageRequest}
     */
    public Either<Throwable, SendMessageRequest> createSendMessageRequest(
            String queueUrl,
            ObjectMapper mapper,
            OpenTelemetryConfig openTelemetryConfig,
            boolean manuallyInjectTrace
    ) {
        SendMessageRequest.Builder builder = SendMessageRequest.builder()
                .queueUrl(queueUrl);

        if (StringUtils.isNotBlank(this.groupId)) {
            builder = builder.messageGroupId(this.groupId);
        }
        if (StringUtils.isNotBlank(this.deduplicationId)) {
            builder = builder.messageDeduplicationId(this.deduplicationId);
        }

        Map<String, String> headers = this.headers;
        if (manuallyInjectTrace && openTelemetryConfig != null) {
            Map<String, String> attributes = new HashMap<>();
            openTelemetryConfig.getOpenTelemetry()
                    .getPropagators()
                    .getTextMapPropagator()
                    .inject(Context.current(), attributes, SETTER);

            headers = Stream.concat(this.headers.entrySet().stream(), attributes.entrySet().stream())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        Map<String, MessageAttributeValue> attributes = headers.entrySet()
                .stream()
                .map(entry ->
                        new AbstractMap.SimpleEntry<>(
                                entry.getKey(),
                                MessageAttributeValue.builder()
                                        .stringValue(entry.getValue())
                                        .dataType("String")
                                        .build())
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        builder = builder.messageAttributes(attributes);

        try {
            String body = this.body.getClass().equals(String.class) ?
                    this.body.toString() :
                    mapper.writeValueAsString(this.body);
            return EitherUtil.convertData(builder.messageBody(body).build());
        } catch (Throwable t) {
            return EitherUtil.convertThrowable(t);
        }
    }
}
