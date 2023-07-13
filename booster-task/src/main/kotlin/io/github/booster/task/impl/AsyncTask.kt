package io.github.booster.task.impl

import arrow.core.Option
import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import reactor.core.publisher.Mono
import java.util.concurrent.ExecutorService

typealias AsyncProcessor<Request, Response> = (Request?) -> Mono<Response>

@Suppress("LongParameterList")
class AsyncTask<Request, Response>(
    name: String,
    executorServiceOption: Option<ExecutorService>,
    retryOption: Option<Retry>,
    circuitBreakerOption: Option<CircuitBreaker>,
    registry: MetricsRegistry,
    private val processor: AsyncProcessor<Request, Response>,
    requestExceptionHandler: RequestExceptionHandler<Response>?
) : AbstractTask<Request, Response>(
    name,
    requestExceptionHandler,
    executorServiceOption,
    retryOption,
    circuitBreakerOption,
    registry
) {
    override fun executeInternal(request: Request?) =
        this.processor.invoke(request)
}

class AsynchronousTaskBuilder<Request, Response> {

    private lateinit var taskName: String
    private var registry: MetricsRegistry = MetricsRegistry()
    private var retryOption: Option<Retry> = Option.fromNullable(null)
    private var circuitBreakerOption: Option<CircuitBreaker> = Option.fromNullable(null)
    private var executorServiceOption: Option<ExecutorService> = Option.fromNullable(null)
    private lateinit var process: AsyncProcessor<Request, Response>
    private var requestExceptionHandler: RequestExceptionHandler<Response>? = null

    fun name(name: String) {
        this.taskName = name
    }

    fun registry(registry: MetricsRegistry) {
        this.registry = registry
    }

    fun processor(process: AsyncProcessor<Request, Response>) {
        this.process = process
    }

    fun exceptionHandler(errorHandler: RequestExceptionHandler<Response>) {
        this.requestExceptionHandler = errorHandler
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
            this.executorServiceOption,
            this.retryOption,
            this.circuitBreakerOption,
            this.registry,
            this.process,
            this.requestExceptionHandler
        )
    }
}

fun <Request, Response> asyncTask(initializer: AsynchronousTaskBuilder<Request, Response>.() -> Unit) =
    AsynchronousTaskBuilder<Request, Response>().apply(initializer)
