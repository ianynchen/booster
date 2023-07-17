package io.github.booster.messaging.publisher.aws;

import com.google.common.base.Preconditions;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.HashMap;
import java.util.Map;

@Getter
@EqualsAndHashCode
@ToString
public class AwsMessage<T> {

    public static class SQSTextMapperSetter implements TextMapSetter<AwsMessage> {

        @Override
        public void set(@Nullable AwsMessage carrier, String key, String value) {
            if (carrier != null && key != null) {
                carrier.headers.put(key, value);
            }
        }
    }

    private final static SQSTextMapperSetter SETTER = new SQSTextMapperSetter();

    private final Map<String, String> headers;

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
        if (groupId != null) {
            this.headers.put("message-group-id", groupId);
        }
        if (deduplicationId != null) {
            this.headers.put("message-deduplication-id", deduplicationId);
        }
        this.body = body;
    }

    public Message<T> toMessage() {
        MessageBuilder<T> builder = MessageBuilder.withPayload(this.body);
        for (String key: this.headers.keySet()) {
            builder.setHeader(key, this.headers.get(key));
        }
        return builder.build();
    }

    public static <T> AwsMessage<T> createMessage(
            OpenTelemetryConfig openTelemetryConfig,
            Map<String, String> headers,
            T body,
            boolean manuallyInjectTrace
    ) {
        return createMessage(
                openTelemetryConfig,
                null,
                null,
                headers,
                body,
                manuallyInjectTrace
        );
    }

    public static <T> AwsMessage<T> createMessage(
            OpenTelemetryConfig openTelemetryConfig,
            String groupId,
            String deduplicationId,
            Map<String, String> headers,
            T body,
            boolean manuallyInjectTrace
    ) {
        AwsMessage<T> message = new AwsMessage<>(groupId, deduplicationId, headers, body);
        if (manuallyInjectTrace && openTelemetryConfig != null){
            openTelemetryConfig.getOpenTelemetry()
                    .getPropagators()
                    .getTextMapPropagator()
                    .inject(Context.current(), message, SETTER);
        }
        return message;
    }
}
