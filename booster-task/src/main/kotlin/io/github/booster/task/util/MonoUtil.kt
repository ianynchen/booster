package io.github.booster.task.util

import arrow.core.Either
import arrow.core.Option
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.task.DataWithError
import io.micrometer.core.instrument.Timer
import org.slf4j.Logger
import reactor.core.publisher.Mono

/**
 * For a [Mono] with possible [Exception]s,
 * record success/failure count based on the result,
 * then convert to [Either] for [Task]s to use.
 * Record total processing time.
 * @param log [Logger] to write logs
 * @param registry [MetricsRegistry] to record time and counter
 * @param sampleOption [Timer.Sample] to record time taken to execute.
 * @param name name of the task to be recorded.
 */
fun <T> Mono<T>.convertAndRecord(
    log: Logger,
    registry: MetricsRegistry,
    sampleOption: Option<Timer.Sample>,
    name: String
): Mono<Either<Throwable, T>> =

    this.map {
        recordSuccessCount(it, log, registry, name)
        log.debug("booster-task - task[{}] produced result: [{}]", name, it)
        val result: Either<Throwable, T> = Either.Right(it)
        result
    }.onErrorResume {
        recordFailureCount(
            it,
            log,
            registry,
            name
        )
        log.warn("booster-task - task [{}] execution produced exception", name, it)
        Mono.just(Either.Left(it))
    }.doOnTerminate {
        recordTime(registry, sampleOption, name)
        log.debug("booster-task - task[{}] terminated", name)
    }
