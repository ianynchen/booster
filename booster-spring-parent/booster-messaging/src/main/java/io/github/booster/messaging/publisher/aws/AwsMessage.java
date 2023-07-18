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

@Getter
@EqualsAndHashCode
@ToString
public class AwsMessage<T> {

    public static class SQSTextMapperSetter implements TextMapSetter<Map<String, String>> {

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

    protected AwsMessage(
            Map<String, String> headers,
            T body
    ) {
        this(null, null, headers, body);
    }

    protected AwsMessage(
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

    public static <T> AwsMessage<T> createMessage(
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

    public static <T> AwsMessage<T> createMessage(
            String groupId,
            String deduplicationId,
            Map<String, String> headers,
            T body
    ) {
        return new AwsMessage<>(groupId, deduplicationId, headers, body);
    }

    public Either<Throwable, SendMessageRequest> createRequest(
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
            Map<String, String> attributes = new HashMap<>(this.headers);
            openTelemetryConfig.getOpenTelemetry()
                    .getPropagators()
                    .getTextMapPropagator()
                    .inject(Context.current(), attributes, SETTER);

            headers = Stream.concat(this.headers.entrySet().stream(), attributes.entrySet().stream())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        Map<String, MessageAttributeValue> attributes = headers.entrySet()
                .stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), MessageAttributeValue.builder().stringValue(entry.getValue()).build()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        builder = builder.messageAttributes(attributes);

        try {
            String body = mapper.writeValueAsString(this.body);
            return EitherUtil.convertData(builder.messageBody(body).build());
        } catch (Throwable t) {
            return EitherUtil.convertThrowable(t);
        }
    }
}
