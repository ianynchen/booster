package io.github.booster.task.impl

import arrow.core.Option
import arrow.core.getOrElse
import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.task.Maybe
import io.github.booster.task.Task
import io.github.booster.task.util.convertAndRecord
import io.vavr.Tuple
import io.vavr.Tuple2
import io.vavr.Tuple4
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

typealias OptionTuple4<E0, E1, E2, E3>
        = Tuple4<Option<E0>, Option<E1>, Option<E2>, Option<E3>>

typealias Tuple4WithError<E0, E1, E2, E3> =
        Tuple4<
                Maybe<E0>,
                Maybe<E1>,
                Maybe<E2>,
                Maybe<E3>
                >

typealias Tuple4ExceptionHandler<Resp0, Resp1, Resp2, Resp3> =
            (Throwable) -> Tuple4WithError<Resp0, Resp1, Resp2, Resp3>

@Suppress("LongParameterList")
class Tuple4Task<Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3>(
    name: String,
    private val task0: Task<Req0, Resp0>,
    private val task1: Task<Req1, Resp1>,
    private val task2: Task<Req2, Resp2>,
    private val task3: Task<Req3, Resp3>,
    private val requestExceptionHandler: Option<Tuple4ExceptionHandler<Resp0, Resp1, Resp2, Resp3>>,
    private val registry: MetricsRegistry
): Task<OptionTuple4<Req0, Req1, Req2, Req3>, Tuple4WithError<Resp0, Resp1, Resp2, Resp3>> {

    private val taskName: String

    init {
        Preconditions.checkArgument(name.isNotBlank(), "task name cannot be blank")
        this.taskName = name
    }

    private fun handleException(t: Throwable): Mono<Option<Tuple4WithError<Resp0, Resp1, Resp2, Resp3>>> {
        return this.requestExceptionHandler.map {
            Mono.just(Option.fromNullable(it.invoke(t)))
        }.getOrElse {
            Mono.error(t)
        }
    }

    private fun executeOnOption(
        tupleOption: Option<OptionTuple4<Req0, Req1, Req2, Req3>>
    ): Mono<Option<Tuple4WithError<Resp0, Resp1, Resp2, Resp3>>> {
        return tupleOption.map { tuple ->
            Mono.zip(
                this.task0.execute(tuple._1()),
                this.task1.execute(tuple._2()),
                this.task2.execute(tuple._3()),
                this.task3.execute(tuple._4())
            )
        }.getOrElse {
            Mono.zip(
                this.task0.execute(Option.fromNullable(null)),
                this.task1.execute(Option.fromNullable(null)),
                this.task2.execute(Option.fromNullable(null)),
                this.task3.execute(Option.fromNullable(null))
            )
        }.map { tuple4 ->
            Option.fromNullable(
                Tuple.of(
                    tuple4.t1,
                    tuple4.t2,
                    tuple4.t3,
                    tuple4.t4
                )
            )
        }
    }

    override fun execute(request: Mono<Maybe<OptionTuple4<Req0, Req1, Req2, Req3>>>):
            Mono<Maybe<Tuple4WithError<Resp0, Resp1, Resp2, Resp3>>> {

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

class Tuple4TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3> {

    private lateinit var taskName: String
    private var registry = MetricsRegistry()
    private lateinit var task0: Task<Req0, Resp0>
    private lateinit var task1: Task<Req1, Resp1>
    private lateinit var task2: Task<Req2, Resp2>
    private lateinit var task3: Task<Req3, Resp3>
    private var requestExceptionHandler: Option<Tuple4ExceptionHandler<Resp0, Resp1, Resp2, Resp3>> =
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

    fun fourthTask(task3: Task<Req3, Resp3>) {
        this.task3 = task3
    }

    fun exceptionHandler(handler: Tuple4ExceptionHandler<Resp0, Resp1, Resp2, Resp3>) {
        this.requestExceptionHandler = Option.fromNullable(handler)
    }

    fun build(): Task<OptionTuple4<Req0, Req1, Req2, Req3>, Tuple4WithError<Resp0, Resp1, Resp2, Resp3>> {
        Preconditions.checkArgument(::taskName.isInitialized, "task name not initialized")
        Preconditions.checkArgument(::task0.isInitialized, "first task not initialized")
        Preconditions.checkArgument(::task1.isInitialized, "second task not initialized")
        Preconditions.checkArgument(::task2.isInitialized, "third task not initialized")
        Preconditions.checkArgument(::task3.isInitialized, "fourth task not initialized")

        return Tuple4Task(
            this.taskName,
            this.task0,
            this.task1,
            this.task2,
            this.task3,
            this.requestExceptionHandler,
            this.registry
        )
    }
}

fun <Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3>
        tuple4Task(initializer: Tuple4TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3>.() -> Unit):
        Tuple4TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3> {
    val builder = Tuple4TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3>()
    builder.apply(initializer)
    return builder
}
