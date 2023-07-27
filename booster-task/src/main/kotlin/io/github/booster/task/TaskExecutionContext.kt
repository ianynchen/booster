package io.github.booster.task

import arrow.core.Option
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import java.util.concurrent.ExecutorService

data class TaskExecutionContext(
    val executorServiceOption: Option<ExecutorService>,
    val retryOption: Option<Retry>,
    val circuitBreakerOption: Option<CircuitBreaker>,
    val registry: MetricsRegistry,
)
