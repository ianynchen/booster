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
import io.vavr.Tuple5
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

typealias OptionTuple5<E0, E1, E2, E3, E4> =
    Tuple5<Option<E0>, Option<E1>, Option<E2>, Option<E3>, Option<E4>>

typealias Tuple5WithError<E0, E1, E2, E3, E4> =
        Tuple5<
                DataWithError<E0>,
                DataWithError<E1>,
                DataWithError<E2>,
                DataWithError<E3>,
                DataWithError<E4>
                >

typealias Tuple5ExceptionHandler<Resp0, Resp1, Resp2, Resp3, Resp4> =
            (Throwable) -> Tuple5WithError<Resp0, Resp1, Resp2, Resp3, Resp4>

@Suppress("LongParameterList")
class Tuple5Task<Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3, Req4, Resp4>(
    name: String,
    private val task0: Task<Req0, Resp0>,
    private val task1: Task<Req1, Resp1>,
    private val task2: Task<Req2, Resp2>,
    private val task3: Task<Req3, Resp3>,
    private val task4: Task<Req4, Resp4>,
    private val requestExceptionHandler: Option<Tuple5ExceptionHandler<Resp0, Resp1, Resp2, Resp3, Resp4>>,
    private val registry: MetricsRegistry
): Task<OptionTuple5<Req0, Req1, Req2, Req3, Req4>,
        Tuple5WithError<Resp0, Resp1, Resp2, Resp3, Resp4>> {

    private val taskName: String

    init {
        Preconditions.checkArgument(name.isNotBlank(), "task name cannot be blank")
        this.taskName = name
    }

    private fun handleException(t: Throwable): Mono<Option<Tuple5WithError<Resp0, Resp1, Resp2, Resp3, Resp4>>> {
        return this.requestExceptionHandler.map {
            Mono.just(Option.fromNullable(it.invoke(t)))
        }.getOrElse {
            Mono.error(t)
        }
    }

    private fun executeOnOption(
        tupleOption: Option<OptionTuple5<Req0, Req1, Req2, Req3, Req4>>
    ): Mono<Option<Tuple5WithError<Resp0, Resp1, Resp2, Resp3, Resp4>>> {
        return tupleOption.map { tuple ->
            Mono.zip(
                this.task0.execute(tuple._1()),
                this.task1.execute(tuple._2()),
                this.task2.execute(tuple._3()),
                this.task3.execute(tuple._4()),
                this.task4.execute(tuple._5())
            )
        }.getOrElse {
            Mono.zip(
                this.task0.execute(Option.fromNullable(null)),
                this.task1.execute(Option.fromNullable(null)),
                this.task2.execute(Option.fromNullable(null)),
                this.task3.execute(Option.fromNullable(null)),
                this.task4.execute(Option.fromNullable(null))
            )
        }.map { tuple5 ->
            Option.fromNullable(
                Tuple.of(
                    tuple5.t1,
                    tuple5.t2,
                    tuple5.t3,
                    tuple5.t4,
                    tuple5.t5
                )
            )
        }
    }

    override fun execute(request: Mono<DataWithError<OptionTuple5<Req0, Req1, Req2, Req3, Req4>>>):
            Mono<DataWithError<Tuple5WithError<Resp0, Resp1, Resp2, Resp3, Resp4>>> {

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
        private val log = LoggerFactory.getLogger(Tuple5Task::class.java)
    }

    override val name: String
        get() = this.taskName
}

class Tuple5TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3, Req4, Resp4> {

    private lateinit var taskName: String
    private var registry = MetricsRegistry()
    private lateinit var task0: Task<Req0, Resp0>
    private lateinit var task1: Task<Req1, Resp1>
    private lateinit var task2: Task<Req2, Resp2>
    private lateinit var task3: Task<Req3, Resp3>
    private lateinit var task4: Task<Req4, Resp4>
    private var requestExceptionHandler: Option<Tuple5ExceptionHandler<Resp0, Resp1, Resp2, Resp3, Resp4>> =
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

    fun fifthTask(task4: Task<Req4, Resp4>) {
        this.task4 = task4
    }

    fun exceptionHandler(handler: Tuple5ExceptionHandler<Resp0, Resp1, Resp2, Resp3, Resp4>) {
        this.requestExceptionHandler = Option.fromNullable(handler)
    }

    fun build(): Task<OptionTuple5<Req0, Req1, Req2, Req3, Req4>, Tuple5WithError<Resp0, Resp1, Resp2, Resp3, Resp4>> {
        Preconditions.checkArgument(::taskName.isInitialized, "task name not initialized")
        Preconditions.checkArgument(::task0.isInitialized, "first task not initialized")
        Preconditions.checkArgument(::task1.isInitialized, "second task not initialized")
        Preconditions.checkArgument(::task2.isInitialized, "third task not initialized")
        Preconditions.checkArgument(::task3.isInitialized, "fourth task not initialized")
        Preconditions.checkArgument(::task4.isInitialized, "fifth task not initialized")

        return Tuple5Task(
            this.taskName,
            this.task0,
            this.task1,
            this.task2,
            this.task3,
            this.task4,
            this.requestExceptionHandler,
            this.registry
        )
    }
}

fun <Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3, Req4, Resp4>
        tuple5Task(initializer: Tuple5TaskBuilder<Req0, Resp0, Req1, Resp1,
        Req2, Resp2, Req3, Resp3, Req4, Resp4>.() -> Unit):
        Tuple5TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3, Req4, Resp4> {
    val builder = Tuple5TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3, Req4, Resp4>()
    builder.apply(initializer)
    return builder
}
