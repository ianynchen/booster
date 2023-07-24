package io.github.booster.messaging.util;

import arrow.core.Option;
import com.google.common.base.Preconditions;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.messaging.MessagingMetricsConstants;
import io.micrometer.core.instrument.Timer;
import org.apache.commons.lang3.StringUtils;

public interface MetricsHelper {

    static void recordProcessingTime(
            MetricsRegistry registry,
            Option<Timer.Sample> sample,
            String timerName,
            String messageType,
            String name
    ) {
        Preconditions.checkArgument(registry != null, "registry cannot be null");
        Preconditions.checkArgument(sample != null, "sample option cannot be null");
        Preconditions.checkArgument(StringUtils.isNotBlank(timerName), "timer name cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(messageType), "message type cannot be blank");

        registry.endSample(
                sample,
                timerName,
                name,
                messageType
        );
    }

    static void recordMessageSubscribeCount(
            MetricsRegistry registry,
            String counterName,
            int count,
            String messageType,
            String name,
            String status,
            String reason
    ) {
        Preconditions.checkArgument(registry != null, "registry cannot be null");
        Preconditions.checkArgument(StringUtils.isNotBlank(counterName), "counter name cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(messageType), "message type cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(status), "status cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(reason), "reason cannot be blank");
        Preconditions.checkArgument(count > 0, "count cannot be less than or equal to 0");

        registry.incrementCounter(
                counterName,
                count,
                MessagingMetricsConstants.NAME, name,
                MessagingMetricsConstants.MESSAGING_TYPE, messageType,
                MessagingMetricsConstants.STATUS, status,
                MessagingMetricsConstants.REASON, reason
        );
    }


    static void recordMessageSubscribeCount(
            MetricsRegistry registry,
            String counterName,
            String messageType,
            String name,
            String status,
            String reason
    ) {
        MetricsHelper.recordMessageSubscribeCount(
                registry,
                counterName,
                1,
                messageType,
                name,
                status,
                reason
        );
    }

    static void recordMessagePublishCount(
            MetricsRegistry registry,
            String counterName,
            String messageType,
            String name,
            String topic,
            String status,
            String reason
    ) {
        Preconditions.checkArgument(registry != null, "registry cannot be null");
        Preconditions.checkArgument(StringUtils.isNotBlank(counterName), "counter name cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(messageType), "message type cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(topic), "topic cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(status), "status cannot be blank");
        Preconditions.checkArgument(StringUtils.isNotBlank(reason), "reason cannot be blank");

        registry.incrementCounter(
                counterName,
                MessagingMetricsConstants.NAME, name,
                MessagingMetricsConstants.TOPIC, topic,
                MessagingMetricsConstants.MESSAGING_TYPE, messageType,
                MessagingMetricsConstants.STATUS, status,
                MessagingMetricsConstants.REASON, reason
        );
    }
}
