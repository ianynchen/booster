package io.github.booster.task.impl

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.task.Task
import io.github.booster.task.util.convertAndRecord
import io.github.booster.task.util.findFirstError
import io.github.booster.task.util.isAnyRight
import io.github.booster.task.util.toScheduler
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import java.util.concurrent.ExecutorService
import java.util.stream.Collectors
import java.util.stream.Stream

typealias ParallelAggregator<T> = (List<Either<Throwable, T?>>) -> List<T?>
typealias ParallelRequestExceptionHandler<T> = (Throwable) -> List<T?>

class ParallelTask<Request, Response>(
    name: String?,
    private val task: Task<Request?, Response>,
    executorServiceOption: Option<ExecutorService>,
    private val errorHandler: ParallelRequestExceptionHandler<Response>?,
    private val aggregateHandler: ParallelAggregator<Response>,
    private val registry: MetricsRegistry,
): Task<List<Request?>, List<Response?>> {

    private val taskName = if (name?.isNotBlank() == true) {
        name
    } else {
        Stream.of("homogeneous", "parallel", task.name).collect(Collectors.joining("_"))
    }

    private val schedulerOption = toScheduler(executorServiceOption)

    private fun handleRequestError(t: Throwable): Mono<List<Response?>> {
        return if (this.errorHandler != null) {
            log.debug("booster-task - task [{}] exception handler provided, processing", this.name)
            Mono.fromSupplier { this.errorHandler.invoke(t) }
        } else {
            log.warn("booster-task - task [{}] no exception handler, throwing exception", this.name, t)
            Mono.error(t)
        }
    }

    private fun process(request: Either<Throwable, List<Request?>?>): Mono<List<Response?>> {
        return if (request.isRight()) {
            log.debug(
                "booster-task - task[{}] processing parallel input: [{}]",
                this.name,
                request
            )
            executeParallel(request.getOrElse { listOf() })
        } else {
            log.warn("booster-task - task[{}] input contains error: [{}]", this.name, request.swap().getOrNull())
            this.handleRequestError(
                request.swap()
                    .getOrElse { IllegalArgumentException("left value without exception") }
            )
        }
    }

    @Suppress("UnsafeCallOnNullableType", "TooGenericExceptionCaught")
    override fun execute(request: Mono<Either<Throwable, List<Request?>?>>): Mono<Either<Throwable, List<Response?>>> {
        val sampleOption = this.registry.startSample()

        return this.schedulerOption.map {
            log.debug("booster-task - task[{}] executing on thread pool", name)
            request.publishOn(it)
        }.getOrElse {
            log.debug("booster-task - task[{}] executing on calling thread", name)
            request
        }.flatMap {
            this.process(it)
        }.convertAndRecord(log, registry, sampleOption, name)
    }

    private fun executeParallel(requests: List<Request?>?): Mono<List<Response?>> {

        val results = requests?.stream()?.map { request -> task.execute(request) }?.collect(Collectors.toList())
        return Mono.zip(results) {
            val convertedResponses = mutableListOf<Either<Throwable, Response?>>()
            it.forEach { item ->
                convertedResponses.add(item as Either<Throwable, Response?>)
            }
            aggregateHandler.invoke(convertedResponses)
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
    private lateinit var elementTask: Task<Request?, Response>
    private var errorHandler: ParallelRequestExceptionHandler<Response>? = null
    private var aggregateHandler: ParallelAggregator<Response> = { values ->
        if (!isAnyRight(values)) {
            throw IllegalArgumentException(findFirstError(values))
        } else {
            values.filter { it.isRight() }.map { it.getOrNull() }
        }
    }

    fun name(name: String?) {
        this.taskName = name
    }

    fun registry(registry: MetricsRegistry) {
        this.metricsRegistry = registry
    }

    fun task(task: Task<Request?, Response>) {
        this.elementTask = task
    }

    fun requestErrorHandler(errorHandler: ParallelRequestExceptionHandler<Response>) {
        this.errorHandler = errorHandler
    }

    fun aggregator(aggregator: ParallelAggregator<Response>) {
        this.aggregateHandler = aggregator
    }

    fun executorOption(executorServiceOption: Option<ExecutorService>) {
        this.executorServiceOption = executorServiceOption
    }

    fun build(): Task<List<Request?>, List<Response?>> {
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
