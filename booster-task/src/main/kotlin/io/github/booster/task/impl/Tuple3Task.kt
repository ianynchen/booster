package io.github.booster.task.impl

import arrow.core.Either
import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.task.Task
import io.github.booster.task.util.convertAndRecord
import io.github.booster.task.util.toMonoEither
import io.vavr.Tuple2
import io.vavr.Tuple3
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

typealias Tuple3ExceptionHandler<Resp0, Resp1, Resp2> = (Throwable) -> Tuple3<Resp0?, Resp1?, Resp2?>
typealias Tuple3Aggregator<Resp0, Resp1, Resp2> =
            (Either<Throwable, Resp0?>,
             Either<Throwable, Resp1?>,
             Either<Throwable, Resp2?>) -> Tuple3<Resp0?, Resp1?, Resp2?>

@Suppress("LongParameterList")
internal class Tuple3Task<Req0, Resp0, Req1, Resp1, Req2, Resp2>(
    name: String,
    private val task0: Task<Req0, Resp0>,
    private val task1: Task<Req1, Resp1>,
    private val task2: Task<Req2, Resp2>,
    private val requestExceptionHandler: Tuple3ExceptionHandler<Resp0, Resp1, Resp2>?,
    private val responseAggregator: Tuple3Aggregator<Resp0, Resp1, Resp2>,
    private val registry: MetricsRegistry
): Task<Tuple3<Req0?, Req1?, Req2?>, Tuple3<Resp0?, Resp1?, Resp2?>> {

    private val taskName: String

    init {
        Preconditions.checkArgument(name.isNotBlank(), "task name cannot be blank")
        this.taskName = name
    }

    private fun handleException(t: Throwable): Mono<Tuple3<Resp0?, Resp1?, Resp2?>> {
        return if (this.requestExceptionHandler == null) {
            Mono.error(t)
        } else {
            Mono.fromSupplier { this.requestExceptionHandler.invoke(t) }
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    override fun execute(request: Mono<Either<Throwable, Tuple3<Req0?, Req1?, Req2?>?>>):
            Mono<Either<Throwable, Tuple3<Resp0?, Resp1?, Resp2?>>> {

        val sampleOption = this.registry.startSample()
        return request.flatMap {
            if (it.isRight()) {
                Mono.zip(
                    task0.execute(toMonoEither(it.getOrNull()?._1())),
                    task1.execute(toMonoEither(it.getOrNull()?._2())),
                    task2.execute(toMonoEither(it.getOrNull()?._3()))
                ).map { tuple -> responseAggregator.invoke(tuple.t1, tuple.t2, tuple.t3) }
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

class Tuple3TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2> {

    private lateinit var taskName: String
    private var registry = MetricsRegistry()
    private lateinit var task0: Task<Req0, Resp0>
    private lateinit var task1: Task<Req1, Resp1>
    private lateinit var task2: Task<Req2, Resp2>
    private lateinit var tupleAggregator: Tuple3Aggregator<Resp0, Resp1, Resp2>
    private var requestExceptionHandler: Tuple3ExceptionHandler<Resp0, Resp1, Resp2>? = null

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

    fun thirdTask(task2: Task<Req2, Resp2>) {
        this.task2 = task2
    }

    fun exceptionHandler(handler: Tuple3ExceptionHandler<Resp0, Resp1, Resp2>) {
        this.requestExceptionHandler = handler
    }

    fun aggregator(aggregator: Tuple3Aggregator<Resp0, Resp1, Resp2>) {
        this.tupleAggregator = aggregator
    }

    fun build(): Task<Tuple3<Req0?, Req1?, Req2?>, Tuple3<Resp0?, Resp1?, Resp2?>> {
        Preconditions.checkArgument(::taskName.isInitialized, "task name not initialized")
        Preconditions.checkArgument(::task0.isInitialized, "first task not initialized")
        Preconditions.checkArgument(::task1.isInitialized, "second task not initialized")
        Preconditions.checkArgument(::task2.isInitialized, "third task not initialized")
        Preconditions.checkArgument(::tupleAggregator.isInitialized, "aggregator not initialized")
        return Tuple3Task(
            this.taskName,
            this.task0,
            this.task1,
            this.task2,
            this.requestExceptionHandler,
            this.tupleAggregator,
            this.registry
        )
    }
}

fun <Req0, Resp0, Req1, Resp1, Req2, Resp2>
        tuple3Task(initializer: Tuple3TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2>.() -> Unit):
        Tuple3TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2> {
    val builder = Tuple3TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2>()
    builder.apply(initializer)
    return builder
}
