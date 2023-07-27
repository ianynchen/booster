package io.github.booster.task.impl

import arrow.core.Either
import arrow.core.Option
import io.github.booster.task.TaskExecutionContext
import io.github.booster.task.circuitBreakerConfig
import io.github.booster.task.defaultLengthFuncObj
import io.github.booster.task.emptyThreadPool
import io.github.booster.task.lengthExceptionThrower
import io.github.booster.task.lengthExceptionThrowerObj
import io.github.booster.task.registry
import io.github.booster.task.retryConfig
import io.github.booster.task.syncLengthFunc
import io.github.booster.task.threadPool
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

internal class SynchronousTaskTest {

    private val taskWithThreadPool: SynchronousTask<String, Int>
        get() = SynchronousTask(
            "lengthTask",
            RequestHandlers(
                Option.fromNullable(defaultLengthFuncObj),
                Option.fromNullable(lengthExceptionThrowerObj)
            ),
            TaskExecutionContext(
                threadPool,
                Option.fromNullable(retryConfig.get("abc")),
                Option.fromNullable(circuitBreakerConfig.get("abc")),
                registry
            )
        ) { Option.fromNullable(it.length) }

    private val taskWithoutThreadPool = SynchronousTask<String, Int>(
        "lengthTask",
        RequestHandlers(
            Option.fromNullable(defaultLengthFuncObj),
            Option.fromNullable(lengthExceptionThrowerObj)
        ),
        TaskExecutionContext(
            emptyThreadPool,
            Option.fromNullable(retryConfig.get("abc")),
            Option.fromNullable(circuitBreakerConfig.get("abc")),
            registry
        )
    ) { Option.fromNullable(it.length) }

    private val completeTask = SynchronousTask<String, Int>(
        "lengthTask",
        RequestHandlers(
            Option.fromNullable(defaultLengthFuncObj),
            Option.fromNullable(lengthExceptionThrowerObj)
        ),
        TaskExecutionContext(
            emptyThreadPool,
            Option.fromNullable(retryConfig.get("test")),
            Option.fromNullable(circuitBreakerConfig.get("test")),
            registry
        )
    ) { Option.fromNullable(it.length) }

    private val defaultExceptionTask = SynchronousTask<String, Int>(
        "lengthTask",
        RequestHandlers(
            Option.fromNullable(defaultLengthFuncObj),
            Option.fromNullable(null)
        ),
        TaskExecutionContext(
            emptyThreadPool,
            Option.fromNullable(retryConfig.get("test")),
            Option.fromNullable(circuitBreakerConfig.get("test")),
            registry
        )
    ) { Option.fromNullable(it.length) }

    @Test
    fun `should run with default exception handling`() {

        StepVerifier.create(defaultExceptionTask.execute("abc"))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.getOrNull()
                assertThat(value, notNullValue())
                assertThat(value?.orNull(), equalTo(3))
            }.verifyComplete()

        StepVerifier.create(defaultExceptionTask.execute(Either.Right<Option<String>>(Option.fromNullable(null))))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.getOrNull()
                assertThat(value, notNullValue())
                assertThat(value?.orNull(), equalTo(0))
            }.verifyComplete()

        StepVerifier.create(defaultExceptionTask.execute(Either.Left(IllegalArgumentException())))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.swap().getOrNull()
                assertThat(value, notNullValue())
                assertThat(value, instanceOf(IllegalArgumentException::class.java))
            }.verifyComplete()
    }

    @Test
    fun `should run with everything`() {

        StepVerifier.create(completeTask.execute("abc"))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.getOrNull()
                assertThat(value, notNullValue())
                assertThat(value?.orNull(), equalTo(3))
            }.verifyComplete()

        StepVerifier.create(completeTask.execute(Either.Right<Option<String>>(Option.fromNullable(null))))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.getOrNull()
                assertThat(value, notNullValue())
                assertThat(value?.orNull(), equalTo(0))
            }.verifyComplete()

        StepVerifier.create(completeTask.execute(Either.Left(IllegalArgumentException())))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.swap().getOrNull()
                assertThat(value, notNullValue())
                assertThat(value, instanceOf(IllegalArgumentException::class.java))
            }.verifyComplete()
    }

    @Test
    fun `should run on calling thread`() {

        StepVerifier.create(taskWithoutThreadPool.execute("abc"))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.getOrNull()
                assertThat(value, notNullValue())
                assertThat(value?.orNull(), equalTo(3))
            }.verifyComplete()

        StepVerifier.create(taskWithoutThreadPool.execute(Either.Right<Option<String>>(Option.fromNullable(null))))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.getOrNull()
                assertThat(value, notNullValue())
                assertThat(value?.orNull(), equalTo(0))
            }.verifyComplete()

        StepVerifier.create(taskWithoutThreadPool.execute(Either.Left(IllegalArgumentException())))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.swap().getOrNull()
                assertThat(value, notNullValue())
                assertThat(value, instanceOf(IllegalArgumentException::class.java))
            }.verifyComplete()
    }

    @Test
    fun `should run on different thread`() {

        StepVerifier.create(taskWithThreadPool.execute("abc"))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.getOrNull()
                assertThat(value, notNullValue())
                assertThat(value?.orNull(), equalTo(3))
            }.verifyComplete()

        StepVerifier.create(taskWithThreadPool.execute(Either.Right<Option<String>>(Option.fromNullable(null))))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.getOrNull()
                assertThat(value, notNullValue())
                assertThat(value?.orNull(), equalTo(0))
            }.verifyComplete()

        StepVerifier.create(taskWithThreadPool.execute(Either.Left(IllegalArgumentException())))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.swap().getOrNull()
                assertThat(value, notNullValue())
                assertThat(value, instanceOf(IllegalArgumentException::class.java))
            }.verifyComplete()
    }

    @Test
    fun `should create simple sync task`() {
        val task = syncTask {
            name("abc")
            registry(registry)
            executorOption(emptyThreadPool)
            retryOption(Option.fromNullable(retryConfig.get("abc")))
            circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
            defaultRequestHandler(defaultLengthFuncObj)
            processor { str: String -> Option.fromNullable(str.length) }
        }.build()

        assertThat(task, notNullValue())

        assertThat(
            syncTask<String, Int> {
                name("abc")
                registry(registry)
                executorOption(emptyThreadPool)
                retryOption(Option.fromNullable(retryConfig.get("abc")))
                circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
                processor { str -> Option.fromNullable(str.length) }
                exceptionHandler { throw it }
            }.build(),
            notNullValue()
        )
    }

    @Test
    fun `should fail create`() {
        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) {
            syncTask<String, Int> {
                registry(registry)
                retryOption(Option.fromNullable(retryConfig.get("abc")))
                circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
                executorOption(emptyThreadPool)
                processor { str -> Option.fromNullable(str.length) }
            }.build()
        }

        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) {
            syncTask<String, Int> {
                registry(registry)
                retryOption(Option.fromNullable(retryConfig.get("abc")))
                circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
                executorOption(emptyThreadPool)
                name("")
                processor { str -> Option.fromNullable(str.length) }
            }.build()
        }

        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) {
            syncTask<String, Int> {
                registry(registry)
                retryOption(Option.fromNullable(retryConfig.get("abc")))
                circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
                executorOption(emptyThreadPool)
                name("  ")
                processor { str: String -> Option.fromNullable(str.length) }
            }.build()
        }

        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) {
            syncTask<String, Int> {
                name("abc")
                registry(registry)
                retryOption(Option.fromNullable(retryConfig.get("abc")))
                circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
                executorOption(emptyThreadPool)
            }.build()
        }
    }

    @Test
    fun `should catch exception`() {

        val task = syncTask<String, Int> {
            name("abc")
            registry(registry)
            retryOption(Option.fromNullable(retryConfig.get("abc")))
            circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
            defaultRequestHandler { throw NullPointerException("") }
            executorOption(emptyThreadPool)
            processor {
                Option.fromNullable(it.length)
            }
        }.build()

        StepVerifier.create(task.execute(Option.fromNullable(null)))
            .consumeNextWith {
                assertThat(it.isLeft(), equalTo(true))
                val error = it.swap().getOrNull()
                assertThat(error, notNullValue())
                assertThat(error, instanceOf(NullPointerException::class.java))
            }.verifyComplete()
    }
}
