package io.github.booster.task.impl

import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.task.Maybe
import io.github.booster.task.Task
import io.github.booster.task.util.recordTime
import reactor.core.publisher.Mono
import java.util.stream.Collectors
import java.util.stream.Stream

class SequentialTask<T1Request, IntermediateResponse, T2Response>(
    name: String?,
    private val task1: Task<T1Request, IntermediateResponse>,
    private val task2: Task<IntermediateResponse, T2Response>,
    private val registry: MetricsRegistry,
): Task<T1Request, T2Response> {

    private val taskName = if (name != null && name.isNotBlank()) {
        name
    } else {
        Stream.of("seq", task1.name, task2.name).collect(Collectors.joining("_"))
    }

    override fun execute(request: Mono<Maybe<T1Request>>): Mono<Maybe<T2Response>> {
        val sampleOption = registry.startSample()

        // since success or failure depends on second task, not
        // recording success/failures in sequential task.

        // since success or failure depends on second task, not
        // recording success/failures in sequential task.
        return task2.execute(task1.execute(request))
            .doOnTerminate { recordTime(registry, sampleOption, this.name) }
    }

    override val name: String
        get() = this.taskName
}

class SequentialTaskBuilder<Request, IntermediateResponse, Response> {

    private var taskName: String? = ""
    private var registry = MetricsRegistry()
    private lateinit var task0: Task<Request, IntermediateResponse>
    private lateinit var task1: Task<IntermediateResponse, Response>

    fun name(name: String) {
        this.taskName = name
    }

    fun registry(registry: MetricsRegistry) {
        this.registry = registry
    }

    fun firstTask(task0: Task<Request, IntermediateResponse>) {
        this.task0 = task0
    }

    fun secondTask(task1: Task<IntermediateResponse, Response>) {
        this.task1 = task1
    }

    fun build(): Task<Request, Response> {
        Preconditions.checkArgument(::task0.isInitialized, "first task not initialized")
        Preconditions.checkArgument(::task1.isInitialized, "second task not initialized")

        return SequentialTask(
            this.taskName,
            this.task0,
            this.task1,
            this.registry
        )
    }
}

fun <Request, IntermediateResponse, Response>
        sequentialTask(initializer: SequentialTaskBuilder<Request, IntermediateResponse, Response>.() -> Unit):
        SequentialTaskBuilder<Request, IntermediateResponse, Response> {
    val builder = SequentialTaskBuilder<Request, IntermediateResponse, Response>()
    builder.apply(initializer)
    return builder
}
