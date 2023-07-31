package io.github.booster.task.impl

import arrow.core.Option
import arrow.core.getOrElse
import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.task.DataWithError
import io.github.booster.task.Task
import io.github.booster.task.util.convertAndRecord
import io.vavr.Tuple
import io.vavr.Tuple2
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono

typealias OptionTuple2<E0, E1> = Tuple2<Option<E0>, Option<E1>>

typealias Tuple2WithError<E0, E1> =
    Tuple2<DataWithError<E0>, DataWithError<E1>>

typealias Tuple2ExceptionHandler<Resp0, Resp1> = (Throwable) -> Tuple2WithError<Resp0, Resp1>
typealias Tuple2Aggregator<Resp0, Resp1> =
        (DataWithError<Resp0>, DataWithError<Resp1>) -> Tuple2WithError<Resp0, Resp1>

internal class Tuple2Task<Req0, Resp0, Req1, Resp1>(
    name: String,
    private val task0: Task<Req0, Resp0>,
    private val task1: Task<Req1, Resp1>,
    private val requestExceptionHandler: Option<Tuple2ExceptionHandler<Resp0, Resp1>>,
    private val registry: MetricsRegistry
): Task<OptionTuple2<Req0, Req1>, Tuple2WithError<Resp0, Resp1>> {

    private val taskName: String

    init {
        Preconditions.checkArgument(name.isNotBlank(), "task name cannot be blank")
        this.taskName = name
    }

    @Suppress("TooGenericExceptionCaught")
    private fun handleException(t: Throwable): Mono<Option<Tuple2WithError<Resp0, Resp1>>> {
        return this.requestExceptionHandler.map {
            Mono.create { sink ->
                try {
                    sink.success(Option.fromNullable(it.invoke(t)))
                } catch (throwable: Throwable) {
                    log.error("booster-task - request exception handler encountered error", throwable)
                    sink.error(throwable)
                }
            }
        }.getOrElse {
            Mono.error(t)
        }
    }

    private fun executeOnOption(
        tupleOption: Option<OptionTuple2<Req0, Req1>>
    ): Mono<Option<Tuple2WithError<Resp0, Resp1>>> {
        return tupleOption.map { tuple ->
            Mono.zip(
                this.task0.execute(tuple._1()),
                this.task1.execute(tuple._2())
            )
        }.getOrElse {
            Mono.zip(
                this.task0.execute(Option.fromNullable(null)),
                this.task1.execute(Option.fromNullable(null))
            )
        }.map { tuple2 ->
            Option.fromNullable(
                Tuple.of(
                    tuple2.t1,
                    tuple2.t2
                )
            )
        }
    }

    override fun execute(
        request: Mono<DataWithError<OptionTuple2<Req0, Req1>>>
    ): Mono<DataWithError<Tuple2WithError<Resp0, Resp1>>> {

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

class Tuple2TaskBuilder<Req0, Resp0, Req1, Resp1> {

    private lateinit var taskName: String
    private var registry = MetricsRegistry()
    private lateinit var task0: Task<Req0, Resp0>
    private lateinit var task1: Task<Req1, Resp1>
    private var requestExceptionHandler: Option<Tuple2ExceptionHandler<Resp0, Resp1>> = Option.fromNullable(null)

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
        this.requestExceptionHandler = Option.fromNullable(handler)
    }

    fun build(): Task<OptionTuple2<Req0, Req1>, Tuple2WithError<Resp0, Resp1>> {
        Preconditions.checkArgument(::taskName.isInitialized, "task name not initialized")
        Preconditions.checkArgument(::task0.isInitialized, "first task not initialized")
        Preconditions.checkArgument(::task1.isInitialized, "second task not initialized")
        return Tuple2Task(
            this.taskName,
            this.task0,
            this.task1,
            this.requestExceptionHandler,
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
