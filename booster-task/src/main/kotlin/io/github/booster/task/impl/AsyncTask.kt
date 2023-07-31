package io.github.booster.task.impl

import arrow.core.Option
import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.task.EmptyRequestHandler
import io.github.booster.task.RequestExceptionHandler
import io.github.booster.task.TaskExecutionContext
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import reactor.core.publisher.Mono
import java.util.concurrent.ExecutorService

typealias AsyncProcessor<Request, Response> = (Request) -> Mono<Option<Response>>

class AsyncTask<Request, Response>(
    name: String,
    requestHandlers: RequestHandlers<Response>,
    taskExecutionContext: TaskExecutionContext,
    private val processor: AsyncProcessor<Request, Response>
) : AbstractTask<Request, Response>(
    name,
    requestHandlers,
    taskExecutionContext
) {
    override fun handleRequest(request: Request): Mono<Option<Response>> =
        this.processor.invoke(request)
}

class AsynchronousTaskBuilder<Request, Response> {

    private lateinit var taskName: String
    private var registry: MetricsRegistry = MetricsRegistry()
    private var retryOption: Option<Retry> = Option.fromNullable(null)
    private var circuitBreakerOption: Option<CircuitBreaker> = Option.fromNullable(null)
    private var executorServiceOption: Option<ExecutorService> = Option.fromNullable(null)
    private lateinit var process: AsyncProcessor<Request, Response>
    private var requestExceptionHandler: Option<RequestExceptionHandler<Response>> = Option.fromNullable(null)
    private var emptyRequestHandler: Option<EmptyRequestHandler<Response>> = Option.fromNullable(null)

    fun name(name: String) {
        this.taskName = name
    }

    fun registry(registry: MetricsRegistry) {
        this.registry = registry
    }

    fun processor(process: AsyncProcessor<Request, Response>) {
        this.process = process
    }

    fun defaultHandler(emptyRequestHandler: EmptyRequestHandler<Response>) {
        this.emptyRequestHandler = Option.fromNullable(emptyRequestHandler)
    }

    fun exceptionHandler(errorHandler: RequestExceptionHandler<Response>) {
        this.requestExceptionHandler = Option.fromNullable(errorHandler)
    }

    fun retryOption(retryOption: Option<Retry>) {
        this.retryOption = retryOption
    }

    fun circuitBreakerOption(circuitBreakerOption: Option<CircuitBreaker>) {
        this.circuitBreakerOption = circuitBreakerOption
    }

    fun executorOption(executorServiceOption: Option<ExecutorService>) {
        this.executorServiceOption = executorServiceOption
    }

    fun build(): io.github.booster.task.Task<Request, Response> {
        Preconditions.checkArgument(::taskName.isInitialized, "task name not initialized")
        Preconditions.checkArgument(::process.isInitialized, "processor not initialized")

        return AsyncTask(
            this.taskName,
            RequestHandlers(
                this.emptyRequestHandler,
                this.requestExceptionHandler
            ),
            TaskExecutionContext(
                this.executorServiceOption,
                this.retryOption,
                this.circuitBreakerOption,
                this.registry
            ),
            this.process,
        )
    }
}

fun <Request, Response> asyncTask(initializer: AsynchronousTaskBuilder<Request, Response>.() -> Unit) =
    AsynchronousTaskBuilder<Request, Response>().apply(initializer)
