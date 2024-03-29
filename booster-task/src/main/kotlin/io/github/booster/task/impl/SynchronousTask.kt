package io.github.booster.task.impl

import arrow.core.Option
import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.task.EmptyRequestHandler
import io.github.booster.task.RequestExceptionHandler
import io.github.booster.task.TaskExecutionContext
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.util.concurrent.ExecutorService

typealias SyncProcessor<Request, Response> = (Request) -> Option<Response>

class SynchronousTask<Request, Response>(
    name: String,
    requestHandlers: RequestHandlers<Response>,
    taskExecutionContext: TaskExecutionContext,
    private val processor: SyncProcessor<Request, Response>,
): AbstractTask<Request, Response>(
    name,
    requestHandlers,
    taskExecutionContext
){
    companion object {
        private val log = LoggerFactory.getLogger(SynchronousTask::class.java)
    }

    @Suppress("TooGenericExceptionCaught")
    override fun handleRequest(request: Request): Mono<Option<Response>> {
        return Mono.create { sink ->
            try {
                val response = this.processor.invoke(request)
                log.debug("booster-task - task[{}] sync processor produced result: [{}]", this.name, response)
                sink.success(response)
            } catch (t: Throwable) {
                log.warn("booster-task - task[{}] sync processor produced exception", this.name, t)
                sink.error(t)
            }
        }
    }
}

class SynchronousTaskBuilder<Request, Response> {

    private lateinit var taskName: String
    private var registry: MetricsRegistry = MetricsRegistry()
    private var retryOption: Option<Retry> = Option.fromNullable(null)
    private var circuitBreakerOption: Option<CircuitBreaker> = Option.fromNullable(null)
    private var executorServiceOption: Option<ExecutorService> = Option.fromNullable(null)
    private lateinit var process: SyncProcessor<Request, Response>
    private var errorHandler: Option<RequestExceptionHandler<Response>> = Option.fromNullable(null)
    private var defaultHandler: Option<EmptyRequestHandler<Response>> = Option.fromNullable(null)

    fun name(name: String) {
        this.taskName = name
    }

    fun registry(registry: MetricsRegistry) {
        this.registry = registry
    }

    fun processor(process: SyncProcessor<Request, Response>) {
        this.process = process
    }

    fun defaultRequestHandler(emptyRequestHandler: EmptyRequestHandler<Response>) {
        this.defaultHandler = Option.fromNullable(emptyRequestHandler)
    }

    fun exceptionHandler(errorHandler: RequestExceptionHandler<Response>) {
        this.errorHandler = Option.fromNullable(errorHandler)
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

        return SynchronousTask(
            this.taskName,
            RequestHandlers(
                this.defaultHandler,
                this.errorHandler
            ),
            TaskExecutionContext(
                this.executorServiceOption,
                this.retryOption,
                this.circuitBreakerOption,
                this.registry,
            ),
            this.process
        )
    }
}

fun <Request, Response> syncTask(initializer: SynchronousTaskBuilder<Request, Response>.() -> Unit) =
    SynchronousTaskBuilder<Request, Response>().apply(initializer)
