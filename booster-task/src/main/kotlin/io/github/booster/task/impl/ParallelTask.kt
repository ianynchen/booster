package io.github.booster.task.impl

import arrow.core.Option
import arrow.core.getOrElse
import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.task.DataWithError
import io.github.booster.task.Task
import io.github.booster.task.util.convertAndRecord
import io.github.booster.task.util.findExisting
import io.github.booster.task.util.toScheduler
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.util.concurrent.ExecutorService
import java.util.stream.Collectors
import java.util.stream.Stream

typealias ParallelAggregator<T> = (List<DataWithError<T>>) -> Option<List<T>>
typealias ParallelRequestExceptionHandler<T> = (Throwable) -> Option<List<T>>

class ParallelTask<Request, Response>(
    name: String?,
    private val task: Task<Request, Response>,
    executorServiceOption: Option<ExecutorService>,
    private val errorHandler: Option<ParallelRequestExceptionHandler<Response>>,
    private val aggregateHandler: Option<ParallelAggregator<Response>>,
    private val registry: MetricsRegistry,
): Task<List<Request>, List<Response>> {

    private val taskName = if (name?.isNotBlank() == true) {
        name
    } else {
        Stream.of("homogeneous", "parallel", task.name)
            .collect(Collectors.joining("_"))
    }

    private val schedulerOption = toScheduler(executorServiceOption)

    private fun handleRequestError(t: Throwable): Mono<Option<List<Response>>> {
        return this.errorHandler.map { handler ->
            log.debug("booster-task - task [{}] exception handler provided, processing", this.name)
            Mono.fromSupplier { handler.invoke(t) }
        }.getOrElse {
            log.warn("booster-task - task [{}] no exception handler, throwing exception", this.name, t)
            Mono.error(t)
        }
    }

    private fun process(request: DataWithError<List<Request>>): Mono<Option<List<Response>>> {
        return request.map { req ->
            log.debug(
                "booster-task - task[{}] processing parallel input: [{}]",
                this.name,
                req
            )
            executeParallel(req)
        }.getOrElse {
            log.warn("booster-task - task[{}] input contains error", this.name, it)
            this.handleRequestError(it)
        }
    }

    @Suppress("UnsafeCallOnNullableType", "TooGenericExceptionCaught")
    override fun execute(request: Mono<DataWithError<List<Request>>>): Mono<DataWithError<List<Response>>> {
        val sampleOption = this.registry.startSample()

        return this.schedulerOption.map {
            log.debug("booster-task - task[{}] executing on thread pool", name)
            request.publishOn(it)
        }.getOrElse {
            log.debug("booster-task - task[{}] executing on calling thread", name)
            request
        }.flatMap {
            this.process(it)
        }.convertAndRecord(log, this.registry, sampleOption, this.name)
    }

    private fun executeParallel(requests: Option<List<Request>>): Mono<Option<List<Response>>> {

        val processedRequests = requests.map { reqs ->
            reqs.map { req -> this.task.execute(req) }
        }.getOrElse {
            listOf(this.task.execute(Option.fromNullable(null)))
        }

        return Mono.zip(processedRequests) {
            val convertedResponses = mutableListOf<DataWithError<Response>>()
            it.forEach { item ->
                val value: DataWithError<Response>? = item as? DataWithError<Response>
                if (value != null) {
                    convertedResponses.add(value)
                }
            }
            this.aggregateHandler.map {
                it.invoke(convertedResponses)
            }.getOrElse {
                val list = findExisting(convertedResponses)
                require(list.isNotEmpty())
                Option.fromNullable(list)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ParallelTask::class.java)
    }

    override val name: String
        get() = this.taskName
}

class ParallelTaskBuilder<Request, Response> {

    private var taskName: String? = ""
    private var metricsRegistry = MetricsRegistry()
    private var executorServiceOption: Option<ExecutorService> = Option.fromNullable(null)
    private lateinit var elementTask: Task<Request, Response>
    private var errorHandler: Option<ParallelRequestExceptionHandler<Response>> = Option.fromNullable(null)
    private var aggregateHandler: Option<ParallelAggregator<Response>> = Option.fromNullable(null)

    fun name(name: String?) {
        this.taskName = name
    }

    fun registry(registry: MetricsRegistry) {
        this.metricsRegistry = registry
    }

    fun task(task: Task<Request, Response>) {
        this.elementTask = task
    }

    fun requestErrorHandler(errorHandler: ParallelRequestExceptionHandler<Response>) {
        this.errorHandler = Option.fromNullable(errorHandler)
    }

    fun aggregator(aggregator: ParallelAggregator<Response>) {
        this.aggregateHandler = Option.fromNullable(aggregator)
    }

    fun executorOption(executorServiceOption: Option<ExecutorService>) {
        this.executorServiceOption = executorServiceOption
    }

    fun build(): Task<List<Request>, List<Response>> {
        Preconditions.checkArgument(::elementTask.isInitialized, "task not initialized")

        return ParallelTask(
            this.taskName,
            this.elementTask,
            this.executorServiceOption,
            this.errorHandler,
            this.aggregateHandler,
            this.metricsRegistry
        )
    }
}

fun <Request, Response> parallelTask(initializer: ParallelTaskBuilder<Request, Response>.() -> Unit):
        ParallelTaskBuilder<Request, Response> {
    val builder = ParallelTaskBuilder<Request, Response>()
    builder.apply(initializer)
    return builder
}
