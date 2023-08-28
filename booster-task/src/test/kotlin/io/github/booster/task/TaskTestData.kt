package io.github.booster.task

import arrow.core.Option
import io.github.booster.commons.circuit.breaker.CircuitBreakerConfig
import io.github.booster.commons.circuit.breaker.CircuitBreakerSetting
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.commons.retry.RetryConfig
import io.github.booster.commons.retry.RetrySetting
import io.github.booster.task.impl.parallelTask
import io.github.booster.task.impl.syncTask
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.util.concurrent.Executors

val threadPool = Option.fromNullable(Executors.newFixedThreadPool(3))

val emptyThreadPool = Option.fromNullable(null)

fun getLinearRetrySetting(): RetrySetting {
    val linearRetrySetting: RetrySetting = RetrySetting()
    linearRetrySetting.backOffPolicy = RetrySetting.BackOffPolicy.LINEAR
    linearRetrySetting.initialBackOffMillis = 10
    linearRetrySetting.maxAttempts = 3
    return linearRetrySetting
}

val retryConfig = RetryConfig(mapOf("test" to getLinearRetrySetting()))

val circuitBreakerSetting = CircuitBreakerSetting()

val circuitBreakerConfig = CircuitBreakerConfig(mapOf("test" to circuitBreakerSetting))

val registry = MetricsRegistry(SimpleMeterRegistry())

val lengthTask = syncTask<String, Int> {
    name("length")
    defaultRequestHandler { Option.fromNullable(0) }
    processor {
        Option.fromNullable(it.length)
    }
}.build()

val stringTask = syncTask<Int, String> {
    name("string")
    defaultRequestHandler { Option.fromNullable("") }
    processor {
        Option.fromNullable(it.toString())
    }
}.build()

val parallelTask = parallelTask {
    task(lengthTask)
}

val sumTask = syncTask<List<Int>, Int> {
    name("sum")
    processor {
        Option.fromNullable(it.sum())
    }
}
