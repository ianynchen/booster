package io.github.booster.task.impl

import arrow.core.Option
import arrow.core.getOrElse
import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.task.DataWithError
import io.github.booster.task.Task
import io.github.booster.task.util.convertAndRecord
import io.github.booster.task.util.extractValue
import io.vavr.Tuple
import io.vavr.Tuple2
import io.vavr.Tuple3
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

typealias OptionTuple3<E0, E1, E2> =
    Tuple3<Option<E0>, Option<E1>, Option<E2>>

typealias Tuple3WithError<E0, E1, E2> =
        Tuple3<
                DataWithError<E0>,
                DataWithError<E1>,
                DataWithError<E2>
                >

typealias Tuple3ExceptionHandler<Resp0, Resp1, Resp2> = (Throwable) -> Tuple3WithError<Resp0, Resp1, Resp2>

@Suppress("LongParameterList")
internal class Tuple3Task<Req0, Resp0, Req1, Resp1, Req2, Resp2>(
    name: String,
    private val task0: Task<Req0, Resp0>,
    private val task1: Task<Req1, Resp1>,
    private val task2: Task<Req2, Resp2>,
    private val requestExceptionHandler: Option<Tuple3ExceptionHandler<Resp0, Resp1, Resp2>>,
    private val registry: MetricsRegistry
): Task<OptionTuple3<Req0, Req1, Req2>, Tuple3WithError<Resp0, Resp1, Resp2>> {

    private val taskName: String

    init {
        Preconditions.checkArgument(name.isNotBlank(), "task name cannot be blank")
        this.taskName = name
    }

    private fun handleException(t: Throwable): Mono<Option<Tuple3WithError<Resp0, Resp1, Resp2>>> {
        return this.requestExceptionHandler.map {
            Mono.just(Option.fromNullable(it.invoke(t)))
        }.getOrElse {
            Mono.error(t)
        }
    }

    private fun executeOnOption(
        tupleOption: Option<OptionTuple3<Req0, Req1, Req2>>
    ): Mono<Option<Tuple3WithError<Resp0, Resp1, Resp2>>> {
        return tupleOption.map { tuple ->
            Mono.zip(
                this.task0.execute(tuple._1()),
                this.task1.execute(tuple._2()),
                this.task2.execute(tuple._3())
            )
        }.getOrElse {
            Mono.zip(
                this.task0.execute(Option.fromNullable(null)),
                this.task1.execute(Option.fromNullable(null)),
                this.task2.execute(Option.fromNullable(null))
            )
        }.map { tuple3 ->
            Option.fromNullable(
                Tuple.of(
                    tuple3.t1,
                    tuple3.t2,
                    tuple3.t3
                )
            )
        }
    }

    override fun execute(request: Mono<DataWithError<OptionTuple3<Req0, Req1, Req2>>>):
            Mono<DataWithError<Tuple3WithError<Resp0, Resp1, Resp2>>> {

        val sampleOption = this.registry.startSample()
        return request.flatMap { req ->
            req.map {
                this.executeOnOption(it)
            }.getOrElse {
                this.handleException(it)
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
    private var requestExceptionHandler: Option<Tuple3ExceptionHandler<Resp0, Resp1, Resp2>> =
        Option.fromNullable(null)

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
        this.requestExceptionHandler = Option.fromNullable(handler)
    }

    fun build(): Task<OptionTuple3<Req0, Req1, Req2>, Tuple3WithError<Resp0, Resp1, Resp2>> {
        Preconditions.checkArgument(::taskName.isInitialized, "task name not initialized")
        Preconditions.checkArgument(::task0.isInitialized, "first task not initialized")
        Preconditions.checkArgument(::task1.isInitialized, "second task not initialized")
        Preconditions.checkArgument(::task2.isInitialized, "third task not initialized")

        return Tuple3Task(
            this.taskName,
            this.task0,
            this.task1,
            this.task2,
            this.requestExceptionHandler,
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
