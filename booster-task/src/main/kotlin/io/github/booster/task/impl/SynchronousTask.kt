package io.github.booster.task.impl

import arrow.core.Option
import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.util.concurrent.ExecutorService

typealias SyncProcessor<Request, Response> = (Request?) -> Response

@Suppress("LongParameterList")
class SynchronousTask<Request, Response>(
    name: String,
    executorServiceOption: Option<ExecutorService>,
    retryOption: Option<Retry>,
    circuitBreakerOption: Option<CircuitBreaker>,
    registry: MetricsRegistry,
    private val processor: SyncProcessor<Request, Response>,
    requestExceptionHandler: RequestExceptionHandler<Response>?
): AbstractTask<Request, Response>(
    name,
    requestExceptionHandler,
    executorServiceOption,
    retryOption,
    circuitBreakerOption,
    registry
){
    companion object {
        private val log = LoggerFactory.getLogger(SynchronousTask::class.java)
    }

    @Suppress("TooGenericExceptionCaught")
    override fun executeInternal(request: Request?): Mono<Response> {
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
    private var errorHandler: RequestExceptionHandler<Response>? = null

    fun name(name: String) {
        this.taskName = name
    }

    fun registry(registry: MetricsRegistry) {
        this.registry = registry
    }

    fun processor(process: SyncProcessor<Request, Response>) {
        this.process = process
    }

    fun exceptionHandler(errorHandler: RequestExceptionHandler<Response>) {
        this.errorHandler = errorHandler
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
            this.executorServiceOption,
            this.retryOption,
            this.circuitBreakerOption,
            this.registry,
            this.process,
            this.errorHandler
        )
    }
}

fun <Request, Response> syncTask(initializer: SynchronousTaskBuilder<Request, Response>.() -> Unit) =
    SynchronousTaskBuilder<Request, Response>().apply(initializer)
