package io.github.booster.task.impl

import arrow.core.Either
import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.task.Task
import io.github.booster.task.util.convertAndRecord
import io.github.booster.task.util.toMonoEither
import io.vavr.Tuple2
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

typealias Tuple2ExceptionHandler<Resp0, Resp1> = (Throwable) -> Tuple2<Resp0?, Resp1?>
typealias Tuple2Aggregator<Resp0, Resp1> =
        (Either<Throwable, Resp0?>, Either<Throwable, Resp1?>) -> Tuple2<Resp0?, Resp1?>

internal class Tuple2Task<Req0, Resp0, Req1, Resp1>(
    name: String,
    private val task0: Task<Req0, Resp0>,
    private val task1: Task<Req1, Resp1>,
    private val requestExceptionHandler: Tuple2ExceptionHandler<Resp0, Resp1>?,
    private val responseAggregator: Tuple2Aggregator<Resp0, Resp1>,
    private val registry: MetricsRegistry
): Task<Tuple2<Req0?, Req1?>, Tuple2<Resp0?, Resp1?>> {

    private val taskName: String

    init {
        Preconditions.checkArgument(name.isNotBlank(), "task name cannot be blank")
        this.taskName = name
    }

    private fun handleException(t: Throwable): Mono<Tuple2<Resp0?, Resp1?>> {
        return if (this.requestExceptionHandler == null) {
            Mono.error(t)
        } else {
            Mono.fromSupplier { this.requestExceptionHandler.invoke(t) }
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    override fun execute(request: Mono<Either<Throwable, Tuple2<Req0?, Req1?>?>>):
            Mono<Either<Throwable, Tuple2<Resp0?, Resp1?>>> {

        val sampleOption = this.registry.startSample()
        return request.flatMap {
            if (it.isRight()) {
                Mono.zip(
                    task0.execute(toMonoEither(it.getOrNull()?._1())),
                    task1.execute(toMonoEither(it.getOrNull()?._2()))
                ).map { tuple -> responseAggregator.invoke(tuple.t1, tuple.t2) }
            } else {
                this.handleException(it.swap().getOrNull()!!)
            }
        }.convertAndRecord(log, registry, sampleOption, name)
    }

    companion object {
        private val log = LoggerFactory.getLogger(Tuple2::class.java)
    }

    override val name: String
        get() = this.taskName
}

class Tuple2TaskBuilder<Req0, Resp0, Req1, Resp1> {

    private lateinit var taskName: String
    private var registry = MetricsRegistry()
    private lateinit var task0: Task<Req0, Resp0>
    private lateinit var task1: Task<Req1, Resp1>
    private lateinit var tupleAggregator: Tuple2Aggregator<Resp0, Resp1>
    private var requestExceptionHandler: Tuple2ExceptionHandler<Resp0, Resp1>? = null

    fun name(name: String) {
        this.taskName = name
    }

    fun registry(registry: MetricsRegistry) {
        this.registry = registry
    }

    fun firstTask(task0: Task<Req0, Resp0>) {
        this.task0 = task0
    }

    fun secondTask(task1: Task<Req1, Resp1>) {
        this.task1 = task1
    }

    fun exceptionHandler(handler: Tuple2ExceptionHandler<Resp0, Resp1>) {
        this.requestExceptionHandler = handler
    }

    fun aggregator(aggregator: Tuple2Aggregator<Resp0, Resp1>) {
        this.tupleAggregator = aggregator
    }

    fun build(): Task<Tuple2<Req0?, Req1?>, Tuple2<Resp0?, Resp1?>> {
        Preconditions.checkArgument(::taskName.isInitialized, "task name not initialized")
        Preconditions.checkArgument(::task0.isInitialized, "first task not initialized")
        Preconditions.checkArgument(::task1.isInitialized, "second task not initialized")
        Preconditions.checkArgument(::tupleAggregator.isInitialized, "aggregator not initialized")
        return Tuple2Task(
            this.taskName,
            this.task0,
            this.task1,
            this.requestExceptionHandler,
            this.tupleAggregator,
            this.registry
        )
    }
}

fun <Req0, Resp0, Req1, Resp1> tuple2Task(initializer: Tuple2TaskBuilder<Req0, Resp0, Req1, Resp1>.() -> Unit):
        Tuple2TaskBuilder<Req0, Resp0, Req1, Resp1> {
    val builder = Tuple2TaskBuilder<Req0, Resp0, Req1, Resp1>()
    builder.apply(initializer)
    return builder
}
