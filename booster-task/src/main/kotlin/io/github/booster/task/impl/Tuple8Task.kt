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
import io.vavr.Tuple8
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono


typealias OptionTuple8<E0, E1, E2, E3, E4, E5, E6, E7> =
        Tuple8<Option<E0>, Option<E1>, Option<E2>, Option<E3>, Option<E4>, Option<E5>, Option<E6>, Option<E7>>

typealias Tuple8WithError<E0, E1, E2, E3, E4, E5, E6, E7> =
        Tuple8<
                DataWithError<E0>,
                DataWithError<E1>,
                DataWithError<E2>,
                DataWithError<E3>,
                DataWithError<E4>,
                DataWithError<E5>,
                DataWithError<E6>,
                DataWithError<E7>
                >

typealias Tuple8ExceptionHandler<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6, Resp7> =
            (Throwable) -> Tuple8WithError<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6, Resp7>

@Suppress("LongParameterList")
class Tuple8Task<Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3,
        Req4, Resp4, Req5, Resp5, Req6, Resp6, Req7, Resp7>(
    name: String,
    private val task0: Task<Req0, Resp0>,
    private val task1: Task<Req1, Resp1>,
    private val task2: Task<Req2, Resp2>,
    private val task3: Task<Req3, Resp3>,
    private val task4: Task<Req4, Resp4>,
    private val task5: Task<Req5, Resp5>,
    private val task6: Task<Req6, Resp6>,
    private val task7: Task<Req7, Resp7>,
    private val requestExceptionHandler:
    Option<Tuple8ExceptionHandler<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6, Resp7>>,
    private val registry: MetricsRegistry
): Task<OptionTuple8<Req0, Req1, Req2, Req3, Req4, Req5, Req6, Req7>,
        Tuple8WithError<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6, Resp7>> {

    private val taskName: String

    init {
        Preconditions.checkArgument(name.isNotBlank(), "task name cannot be blank")
        this.taskName = name
    }

    private fun handleException(t: Throwable):
            Mono<Option<Tuple8WithError<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6, Resp7>>> {
        return this.requestExceptionHandler.map {
            Mono.just(Option.fromNullable(it.invoke(t)))
        }.getOrElse {
            Mono.error(t)
        }
    }

    private fun executeOnOption(
        tupleOption: Option<OptionTuple8<Req0, Req1, Req2, Req3, Req4, Req5, Req6, Req7>>
    ): Mono<Option<Tuple8WithError<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6, Resp7>>> {
        return tupleOption.map { tuple ->
            Mono.zip(
                this.task0.execute(tuple._1()),
                this.task1.execute(tuple._2()),
                this.task2.execute(tuple._3()),
                this.task3.execute(tuple._4()),
                this.task4.execute(tuple._5()),
                this.task5.execute(tuple._6()),
                this.task6.execute(tuple._7()),
                this.task7.execute(tuple._8())
            )
        }.getOrElse {
            Mono.zip(
                this.task0.execute(Option.fromNullable(null)),
                this.task1.execute(Option.fromNullable(null)),
                this.task2.execute(Option.fromNullable(null)),
                this.task3.execute(Option.fromNullable(null)),
                this.task4.execute(Option.fromNullable(null)),
                this.task5.execute(Option.fromNullable(null)),
                this.task6.execute(Option.fromNullable(null)),
                this.task7.execute(Option.fromNullable(null))
            )
        }.map { tuple8 ->
            Option.fromNullable(
                Tuple.of(
                    tuple8.t1,
                    tuple8.t2,
                    tuple8.t3,
                    tuple8.t4,
                    tuple8.t5,
                    tuple8.t6,
                    tuple8.t7,
                    tuple8.t8
                )
            )
        }
    }

    override fun execute(request: Mono<DataWithError<OptionTuple8<Req0, Req1, Req2, Req3, Req4, Req5, Req6, Req7>>>):
            Mono<DataWithError<Tuple8WithError<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6, Resp7>>> {

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
        private val log = LoggerFactory.getLogger(Tuple8Task::class.java)
    }

    override val name: String
        get() = this.taskName
}

@Suppress("TooManyFunctions")
class Tuple8TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3,
        Req4, Resp4, Req5, Resp5, Req6, Resp6, Req7, Resp7> {

    private lateinit var taskName: String
    private var registry = MetricsRegistry()
    private lateinit var task0: Task<Req0, Resp0>
    private lateinit var task1: Task<Req1, Resp1>
    private lateinit var task2: Task<Req2, Resp2>
    private lateinit var task3: Task<Req3, Resp3>
    private lateinit var task4: Task<Req4, Resp4>
    private lateinit var task5: Task<Req5, Resp5>
    private lateinit var task6: Task<Req6, Resp6>
    private lateinit var task7: Task<Req7, Resp7>
    private var requestExceptionHandler:
            Option<Tuple8ExceptionHandler<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6, Resp7>> =
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

    fun sixthTask(task5: Task<Req5, Resp5>) {
        this.task5 = task5
    }

    fun seventhTask(task6: Task<Req6, Resp6>) {
        this.task6 = task6
    }

    fun eighthTask(task7: Task<Req7, Resp7>) {
        this.task7 = task7
    }

    fun exceptionHandler(handler: Tuple8ExceptionHandler<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6, Resp7>) {
        this.requestExceptionHandler = Option.fromNullable(handler)
    }

    fun build(): Task<
            OptionTuple8<Req0, Req1, Req2, Req3, Req4, Req5, Req6, Req7>,
            Tuple8WithError<Resp0, Resp1, Resp2, Resp3, Resp4, Resp5, Resp6, Resp7>> {
        Preconditions.checkArgument(::taskName.isInitialized, "task name not initialized")
        Preconditions.checkArgument(::task0.isInitialized, "first task not initialized")
        Preconditions.checkArgument(::task1.isInitialized, "second task not initialized")
        Preconditions.checkArgument(::task2.isInitialized, "third task not initialized")
        Preconditions.checkArgument(::task3.isInitialized, "fourth task not initialized")
        Preconditions.checkArgument(::task4.isInitialized, "fifth task not initialized")
        Preconditions.checkArgument(::task5.isInitialized, "sixth task not initialized")
        Preconditions.checkArgument(::task6.isInitialized, "seventh task not initialized")
        Preconditions.checkArgument(::task7.isInitialized, "eighth task not initialized")

        return Tuple8Task(
            this.taskName,
            this.task0,
            this.task1,
            this.task2,
            this.task3,
            this.task4,
            this.task5,
            this.task6,
            this.task7,
            this.requestExceptionHandler,
            this.registry
        )
    }
}

fun <Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3,
        Req4, Resp4, Req5, Resp5, Req6, Resp6, Req7, Resp7>
        tuple8Task(initializer: Tuple8TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2,
        Req3, Resp3, Req4, Resp4, Req5, Resp5, Req6, Resp6, Req7, Resp7>.() -> Unit):
        Tuple8TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3,
                Req4, Resp4, Req5, Resp5, Req6, Resp6, Req7, Resp7> {
    val builder = Tuple8TaskBuilder<Req0, Resp0, Req1, Resp1, Req2, Resp2, Req3, Resp3,
            Req4, Resp4, Req5, Resp5, Req6, Resp6, Req7, Resp7>()
    builder.apply(initializer)
    return builder
}
