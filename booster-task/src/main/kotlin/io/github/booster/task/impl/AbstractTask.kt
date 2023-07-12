package io.github.booster.task.impl

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.task.Task
import io.github.booster.task.util.convertAndRecord
import io.github.booster.task.util.toScheduler
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator
import io.github.resilience4j.reactor.retry.RetryOperator
import io.github.resilience4j.retry.Retry
import io.micrometer.core.instrument.Timer
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import java.util.concurrent.ExecutorService

typealias RequestExceptionHandler<T> = (Throwable) -> T

/**
 * Base class for all tasks. Every task being executed supports optional
 * [Retry] and [CircuitBreaker].
 * @param <Request> Request object type.
 * @param <Response> Response object type.
</Response></Request> */
abstract class AbstractTask<Request, Response>(
    name: String,
    private val requestExceptionHandler: RequestExceptionHandler<Response>?,
    executorServiceOption: Option<ExecutorService>,
    retryOption: Option<Retry>,
    circuitBreakerOption: Option<CircuitBreaker>,
    registry: MetricsRegistry,
) : Task<Request, Response> {
    private val taskName: String
    private val scheduler: Option<Scheduler>
    private val registry: MetricsRegistry
    private val retryOption: Option<Retry>
    private val circuitBreakerOption: Option<CircuitBreaker>

    /**
     * Constructor
     * @param name name of the task.
     * @param executorServiceOption optional thread to run the task on. if absent, the executor is run on calling thread
     * @param retryOption optional [Retry] to allow retry of current task. If not provided, no retry will be attempted.
     * @param circuitBreakerOption optional [CircuitBreaker] to allow circuit breaking on current task. If
     * not provided, no circuit breaker will be used.
     * @param registry [MetricsRegistry] to record metrics.
     */
    init {
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "name cannot be blank")
        this.taskName = name
        this.registry = registry
        // Create a scheduler from ExecutorService, then monitor it for metrics.
        scheduler = toScheduler(executorServiceOption)
        this.retryOption = retryOption
        this.circuitBreakerOption = circuitBreakerOption
    }

    /**
     * Internal execution logic for [Task]. No error handling required.
     * @param request request
     * @return a [Mono] of execution result.
     */
    @Suppress("UnsafeCallOnNullableType")
    private fun executeInternal(request: Either<Throwable, Request?>): Mono<Response> {
        return if (request.isRight()) {
            log.debug("booster-task - task[{}] running with request values: [{}]", name, request.getOrNull())
            var response = this.executeInternal(request.getOrNull())

            // if has retry, add it.
            response = this.retryOption.map { retry ->
                log.debug(
                    "booster-task - task[{}] enabling retry: {}",
                    name,
                    retryOption.getOrElse { null }
                )
                response.transformDeferred(RetryOperator.of(retry))
            }.getOrElse {
                log.debug("booster-task - task[{}] withtout retry", name)
                response
            }

            // if has circuit breaker, add it.
            response = this.circuitBreakerOption.map { circuitBreaker ->
                log.debug(
                    "booster-task - task[{}] enabling circuit breaker: {}",
                    name,
                    circuitBreakerOption.getOrElse { null }
                )
                response.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            }.getOrElse {
                log.debug("booster-task - task[{}] without circuit breaker", name)
                response
            }
            response
        } else {
            log.warn("booster-task - task[{}] input contains exception", name, request.swap().getOrNull())
            Mono.fromSupplier { handleRequestException(request.swap().getOrNull()!!) }
        }
    }

    private fun handleRequestException(t: Throwable): Response {
        return if (this.requestExceptionHandler != null) {
            log.warn("booster-task - task[{}] invoking request exception handler", name, t)
            this.requestExceptionHandler.invoke(t)
        } else {
            log.warn("booster-task - task[{}] no request exception handler, throwing exception", name, t)
            throw t
        }
    }

    protected abstract fun executeInternal(request: Request?): Mono<Response>

    override fun execute(request: Mono<Either<Throwable, Request?>>): Mono<Either<Throwable, Response>> {
        val sampleOption: Option<Timer.Sample> = registry.startSample()

        // To execute on thread provided, or calling thread.
        return this.scheduler.map {
            log.debug("booster-task - task[{}] using thread pool", name)
            request.publishOn(it)
        }.getOrElse {
            log.debug("booster-task - task[{}] using calling thread", name)
            request
        }.flatMap {
            this.executeInternal(it)
        }.convertAndRecord(log, registry, sampleOption, name)
    }

    companion object {
        private val log = LoggerFactory.getLogger(AbstractTask::class.java)
    }

    override val name: String
        get() = this.taskName
}
