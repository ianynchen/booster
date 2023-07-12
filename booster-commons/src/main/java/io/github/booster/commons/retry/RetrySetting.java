package io.github.booster.commons.retry;

import arrow.core.Option;
import arrow.core.OptionKt;
import com.google.common.base.Preconditions;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.micrometer.tagged.TaggedRetryMetrics;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;

/**
 * Retry config to create retries.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RetrySetting {

    /**
     * retry backoff policy, either using linear backoff time or exponentially increasing backoff time.
     */
    public enum BackOffPolicy {
        /**
         * Linear backoff time.
         */
        LINEAR,
        /**
         * Exponential backoff time.
         */
        EXPONENTIAL,
    }

    /**
     * Default initial backoff time in milliseconds. This
     * value is used if no valid initial backoff time is
     * provided.
     */
    public static final int DEFAULT_INITIAL_BACKOFF_MILLIS = 100;

    /**
     * Minimum initial backoff time in milliseconds.
     */
    public static final int MINIMUM_INITIAL_BACKOFF_MILLIS = 1;

    private BackOffPolicy backOffPolicy;

    private int maxAttempts;

    private int initialBackOffMillis;

    public void setBackOffPolicy(BackOffPolicy backOffPolicy) {
        this.backOffPolicy = backOffPolicy == null ? BackOffPolicy.LINEAR : backOffPolicy;
    }

    /**
     * Backoff policy, either linear or exponential.
     * @return backoff policy, either linear or exponential.
     */
    public BackOffPolicy getBackOffPolicy() {
        return this.backOffPolicy == null ? BackOffPolicy.LINEAR : this.backOffPolicy;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts < 0 ? 0 : maxAttempts;
    }

    /**
     * Total number of client calls, including the initial one.
     *
     * @return maximum attempts.
     */
    public int getMaxAttempts() {
        return this.maxAttempts <= 0 ? 0 : this.maxAttempts;
    }

    public void setInitialBackOffMillis(int initialBackOffMillis) {
        this.initialBackOffMillis = initialBackOffMillis < MINIMUM_INITIAL_BACKOFF_MILLIS ? DEFAULT_INITIAL_BACKOFF_MILLIS : initialBackOffMillis;
    }

    /**
     * Initial backoff time in milliseconds.
     * @return initial backoff milliseconds.
     */
    public int getInitialBackOffMillis() {
        return this.initialBackOffMillis < MINIMUM_INITIAL_BACKOFF_MILLIS ? DEFAULT_INITIAL_BACKOFF_MILLIS : initialBackOffMillis;
    }

    /**
     * Builds a resilience4j Retry using name. No metrics will be reported.
     * @param name name of {@link Retry}
     * @return optional {@link Retry}
     */
    public Option<Retry> buildRetry(String name) {
        return buildRetry(name, null);
    }

    /**
     * Builds a resilience4j Retry using name, will also record metrics.
     * @param name name of {@link Retry}
     * @param metricsRegistry {@link MetricsRegistry} to record metrics.
     * @return optional {@link Retry}
     */
    public Option<Retry> buildRetry(String name, MetricsRegistry metricsRegistry) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "name cannot be null");

        if (this.getMaxAttempts() == 0) {
            return Option.fromNullable(null);
        }

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(this.getMaxAttempts())
                .intervalFunction(this.getBackOffPolicy() == BackOffPolicy.LINEAR ?
                        IntervalFunction.of(Duration.ofMillis(this.getInitialBackOffMillis())) :
                        IntervalFunction.ofExponentialBackoff(Duration.ofMillis(this.getInitialBackOffMillis())))
                .build();
        RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);
        if (metricsRegistry != null && metricsRegistry.getRegistry().isDefined()) {
            TaggedRetryMetrics.ofRetryRegistry(retryRegistry)
                    .bindTo(OptionKt.getOrElse(metricsRegistry.getRegistry(), () -> null));
        }
        return Option.fromNullable(retryRegistry.retry(name, retryConfig));
    }
}
