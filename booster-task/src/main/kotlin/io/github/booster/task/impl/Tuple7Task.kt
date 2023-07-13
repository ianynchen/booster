package io.github.booster.task.impl

import arrow.core.Either
import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.task.Task
import io.github.booster.task.util.convertAndRecord
import io.github.booster.task.util.toMonoEither
import io.vavr.Tuple7
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

typealias Tuple7ExceptionHandler<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6> =
            (Throwable) -> Tuple7<Resp0?, Resp1?, Resp2?, Resp3?, Resp4?, Resp5?, Resp6?>
typealias Tuple7Aggregator<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6> =
            (
            Either<Throwable, Resp0?>, Either<Throwable, Resp1?>,
            Either<Throwable, Resp2?>, Either<Throwable, Resp3?>,
            Either<Throwable, Resp4?>, Either<Throwable, Resp5?>,
            Either<Throwable, Resp6?>
        ) -> Tuple7<Resp0?, Resp1?, Resp2?, Resp3?, Resp4?, Resp5?, Resp6?>

@Suppress("LongParameterList")
class Tuple7Task<Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3, Req4, Resp4, Req5, Resp5, Req6, Resp6>(
    name: String,
    private val task0: Task<Req0, Resp0>,
    private val task1: Task<Req1, Resp1>,
    private val task2: Task<Req2, Resp2>,
    private val task3: Task<Req3, Resp3>,
    private val task4: Task<Req4, Resp4>,
    private val task5: Task<Req5, Resp5>,
    private val task6: Task<Req6, Resp6>,
    private val requestExceptionHandler: Tuple7ExceptionHandler<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6>?,
    private val responseAggregator: Tuple7Aggregator<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6>,
    private val registry: MetricsRegistry
): Task<Tuple7<Req0?, Req1?, Req2?, Req3?, Req4?, Req5?, Req6?>,
        Tuple7<Resp0?, Resp1?, Resp2?, Resp3?, Resp4?, Resp5?, Resp6?>> {

    private val taskName: String

    init {
        Preconditions.checkArgument(name.isNotBlank(), "task name cannot be blank")
        this.taskName = name
    }

    private fun handleException(t: Throwable): Mono<Tuple7<Resp0?, Resp1?, Resp2?, Resp3?, Resp4?, Resp5?, Resp6?>> {
        return if (this.requestExceptionHandler == null) {
            Mono.error(t)
        } else {
            Mono.fromSupplier { this.requestExceptionHandler.invoke(t) }
        }
    }

    @Suppress("UnsafeCallOnNullableType")
    override fun execute(request: Mono<Either<Throwable, Tuple7<Req0?, Req1?, Req2?, Req3?, Req4?, Req5?, Req6?>?>>):
            Mono<Either<Throwable, Tuple7<Resp0?, Resp1?, Resp2?, Resp3?, Resp4?, Resp5?, Resp6?>>> {

        val sampleOption = this.registry.startSample()
        return request.flatMap {
            if (it.isRight()) {
                Mono.zip(
                    task0.execute(toMonoEither(it.getOrNull()?._1())),
                    task1.execute(toMonoEither(it.getOrNull()?._2())),
                    task2.execute(toMonoEither(it.getOrNull()?._3())),
                    task3.execute(toMonoEither(it.getOrNull()?._4())),
                    task4.execute(toMonoEither(it.getOrNull()?._5())),
                    task5.execute(toMonoEither(it.getOrNull()?._6())),
                    task6.execute(toMonoEither(it.getOrNull()?._7()))
                ).map { tuple -> responseAggregator.invoke(tuple.t1, tuple.t2, tuple.t3,
                    tuple.t4, tuple.t5, tuple.t6, tuple.t7) }
            } else {
                this.handleException(it.swap().getOrNull()!!)
            }
        }.convertAndRecord(log, registry, sampleOption, name)
    }

    companion object {
        private val log = LoggerFactory.getLogger(Tuple7Task::class.java)
    }

    override val name: String
        get() = this.taskName
}

@Suppress("TooManyFunctions")
class Tuple7TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3, Req4, Resp4, Req5, Resp5, Req6, Resp6> {

    private lateinit var taskName: String
    private var registry = MetricsRegistry()
    private lateinit var task0: Task<Req0, Resp0>
    private lateinit var task1: Task<Req1, Resp1>
    private lateinit var task2: Task<Req2, Resp2>
    private lateinit var task3: Task<Req3, Resp3>
    private lateinit var task4: Task<Req4, Resp4>
    private lateinit var task5: Task<Req5, Resp5>
    private lateinit var task6: Task<Req6, Resp6>
    private lateinit var tupleAggregator: Tuple7Aggregator<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6>
    private var requestExceptionHandler: Tuple7ExceptionHandler<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6>? = null

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

    fun fourthTask(task3: Task<Req3, Resp3>) {
        this.task3 = task3
    }

    fun fifthTask(task4: Task<Req4, Resp4>) {
        this.task4 = task4
    }

    fun sixthTask(task5: Task<Req5, Resp5>) {
        this.task5 = task5
    }

    fun seventhTask(task6: Task<Req6, Resp6>) {
        this.task6 = task6
    }

    fun exceptionHandler(handler: Tuple7ExceptionHandler<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6>) {
        this.requestExceptionHandler = handler
    }

    fun aggregator(aggregator: Tuple7Aggregator<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6>) {
        this.tupleAggregator = aggregator
    }

    fun build(): Task<
            Tuple7<Req0?, Req1?, Req2?, Req3?, Req4?, Req5?, Req6?>,
            Tuple7<Resp0?, Resp1?, Resp2?, Resp3?, Resp4?, Resp5?, Resp6?>
            > {
        Preconditions.checkArgument(::taskName.isInitialized, "task name not initialized")
        Preconditions.checkArgument(::task0.isInitialized, "first task not initialized")
        Preconditions.checkArgument(::task1.isInitialized, "second task not initialized")
        Preconditions.checkArgument(::task2.isInitialized, "third task not initialized")
        Preconditions.checkArgument(::task3.isInitialized, "fourth task not initialized")
        Preconditions.checkArgument(::task4.isInitialized, "fifth task not initialized")
        Preconditions.checkArgument(::task5.isInitialized, "sixth task not initialized")
        Preconditions.checkArgument(::task6.isInitialized, "seventh task not initialized")
        Preconditions.checkArgument(::tupleAggregator.isInitialized, "aggregator not initialized")
        return Tuple7Task(
            this.taskName,
            this.task0,
            this.task1,
            this.task2,
            this.task3,
            this.task4,
            this.task5,
            this.task6,
            this.requestExceptionHandler,
            this.tupleAggregator,
            this.registry
        )
    }
}

fun <Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3,
        Req4, Resp4, Req5, Resp5, Req6, Resp6>
        tuple7Task(initializer: Tuple7TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2,
        Req3, Resp3, Req4, Resp4, Req5, Resp5, Req6, Resp6>.() -> Unit):
        Tuple7TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3,
                Req4, Resp4, Req5, Resp5, Req6, Resp6> {
    val builder = Tuple7TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2,
            Req3, Resp3, Req4, Resp4, Req5, Resp5, Req6, Resp6>()
    builder.apply(initializer)
    return builder
}
