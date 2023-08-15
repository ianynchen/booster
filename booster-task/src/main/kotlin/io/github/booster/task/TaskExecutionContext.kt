package io.github.booster.task

import arrow.core.Option
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import java.util.concurrent.ExecutorService

enum class ExecutionType {
    PUBLISH_ON,
    SUBSCRIBE_ON
}

/**
 * A task execution context
 * @param executorServiceOption [Option] of [ExecutorService]
 * @param retryOption [Option] of [Retry]
 * @param circuitBreakerOption [Option] of [CircuitBreaker]
 * @param registry [MetricsRegistry]
 * @param executionType whether to publish on thread or subscribe on thread.
 */
data class TaskExecutionContext @JvmOverloads constructor(
    val executorServiceOption: Option<ExecutorService>,
    val retryOption: Option<Retry>,
    val circuitBreakerOption: Option<CircuitBreaker>,
    val registry: MetricsRegistry,
    val executionType: ExecutionType = ExecutionType.PUBLISH_ON
)
