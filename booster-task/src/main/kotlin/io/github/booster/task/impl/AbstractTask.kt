package io.github.booster.task.impl

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.orElse
import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.task.Maybe
import io.github.booster.task.EmptyRequestHandler
import io.github.booster.task.ExecutionType
import io.github.booster.task.RequestExceptionHandler
import io.github.booster.task.Task
import io.github.booster.task.TaskExecutionContext
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

/**
 * Default handlers for request edge cases
 * @param emptyRequestHandler handler for empty request option
 * @param requestExceptionHandler handler for request exception
 */
data class RequestHandlers<Response>(
    val emptyRequestHandler: Option<EmptyRequestHandler<Response>>,
    val requestExceptionHandler: Option<RequestExceptionHandler<Response>>
)

/**
 * Base class for all tasks. Every task being executed supports optional
 * [Retry] and [CircuitBreaker].
 * @param [Request] Request object type.
 * @param [Response] Response object type.
 */
abstract class AbstractTask<Request, Response>(
    name: String,
    private val requestHandlers: RequestHandlers<Response>,
    private val taskExecutionContext: TaskExecutionContext
) : Task<Request, Response> {
    private val taskName: String
    private val scheduler: Option<Scheduler>

    /**
     * Constructor
     */
    init {
        Preconditions.checkArgument(name.isNotBlank(), "name cannot be blank")
        this.taskName = name
        // Create a scheduler from ExecutorService, then monitor it for metrics.
        this.scheduler = toScheduler(taskExecutionContext.executorServiceOption)
    }

    /**
     * Internal execution logic for [Task]. No error handling required.
     * @param request request
     * @return a [Mono] of execution result.
     */
    //@Suppress("UnsafeCallOnNullableType")
    private fun executeInternal(request: Maybe<Request>): Mono<Option<Response>> {

        return request.map { req ->
            log.debug("booster-task - task[{}] running with optional request values: [{}]", name, req)
            var response = req.map {
                this.handleRequest(it)
            }.getOrElse {
                // handles empty requests with [RequestHandlers#emptyRequestHandler]
                Mono.just(
                    this.requestHandlers.emptyRequestHandler.map {
                        it.invoke()
                    }.getOrElse {
                        Option.fromNullable(null)
                    }
                )
            }

            // if has retry, add it.
            response = this.taskExecutionContext.retryOption.map { retry ->
                log.debug(
                    "booster-task - task[{}] enabling retry: {}",
                    name,
                    this.taskExecutionContext.retryOption.getOrElse { null }
                )
                response.transformDeferred(RetryOperator.of(retry))
            }.getOrElse {
                log.debug("booster-task - task[{}] without retry", name)
                response
            }

            // if has circuit breaker, add it.
            response = this.taskExecutionContext.circuitBreakerOption.map { circuitBreaker ->
                log.debug(
                    "booster-task - task[{}] enabling circuit breaker: {}",
                    name,
                    this.taskExecutionContext.circuitBreakerOption.getOrElse { null }
                )
                response.transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
            }.getOrElse {
                log.debug("booster-task - task[{}] without circuit breaker", name)
                response
            }
            response
        }.getOrElse {
            // handles request exceptions.
            log.warn("booster-task - task[{}] input has exception", name, it)
            Mono.fromSupplier { handleRequestException(it) }
        }
    }

    protected abstract fun handleRequest(request: Request): Mono<Option<Response>>

    private fun handleRequestException(t: Throwable): Option<Response> {
        return this.requestHandlers.requestExceptionHandler.flatMap {
            log.warn("booster-task - task[{}] invoking request exception handler", name, t)
            it.invoke(t)
        }.orElse {
            log.warn("booster-task - task[{}] no request exception handler, throwing exception", name, t)
            throw t
        }
    }

    override fun execute(request: Mono<Maybe<Request>>): Mono<Maybe<Response>> {
        val sampleOption: Option<Timer.Sample> = this.taskExecutionContext.registry.startSample()

        return if (this.taskExecutionContext.executionType == ExecutionType.PUBLISH_ON) {
            // To execute on thread provided, or calling thread.
            this.scheduler.map {
                log.debug("booster-task - task[{}] using thread pool", name)
                request.publishOn(it)
            }.getOrElse {
                log.debug("booster-task - task[{}] using calling thread", name)
                request
            }.flatMap {
                this.executeInternal(it)
            }
        } else {
            this.scheduler.map {
                request.flatMap { req -> this.executeInternal(req) }.subscribeOn(it)
            }.getOrElse {
                request.flatMap {
                    this.executeInternal(it)
                }
            }
        }.convertAndRecord(log, this.taskExecutionContext.registry, sampleOption, name)
    }

    companion object {
        private val log = LoggerFactory.getLogger(AbstractTask::class.java)
    }

    override val name: String
        get() = this.taskName
}
