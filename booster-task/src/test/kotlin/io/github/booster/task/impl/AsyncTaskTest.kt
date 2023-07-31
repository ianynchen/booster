package io.github.booster.task.impl

import arrow.core.Either
import arrow.core.Option
import io.github.booster.task.TaskExecutionContext
import io.github.booster.task.asyncLengthFunc
import io.github.booster.task.defaultLengthFuncObj
import io.github.booster.task.circuitBreakerConfig
import io.github.booster.task.emptyThreadPool
import io.github.booster.task.lengthExceptionThrowerObj
import io.github.booster.task.registry
import io.github.booster.task.retryConfig
import io.github.booster.task.threadPool
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

internal class AsyncTaskTest {

    private val taskWithThreadPool = AsyncTask(
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
        ),
        ::asyncLengthFunc
    )

    private val taskWithoutThreadPool = AsyncTask(
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
        ),
        ::asyncLengthFunc
    )

    private val completeTask = AsyncTask(
        "lengthTask",
        RequestHandlers(
            Option.fromNullable(defaultLengthFuncObj),
            Option.fromNullable(lengthExceptionThrowerObj)
        ),
        TaskExecutionContext(
            emptyThreadPool,
            Option.fromNullable(retryConfig.get("test")),
            Option.fromNullable(circuitBreakerConfig.get("test")),
            registry,
        ),
        ::asyncLengthFunc
    )

    private val defaultExceptionTask = AsyncTask<String, Int>(
        "lengthTask",
        RequestHandlers(
            Option.fromNullable(defaultLengthFuncObj),
            Option.fromNullable(null)
        ),
        TaskExecutionContext(
            emptyThreadPool,
            Option.fromNullable(retryConfig.get("test")),
            Option.fromNullable(circuitBreakerConfig.get("test")),
            registry,
        ),
        ::asyncLengthFunc
    )

    @Test
    fun `should run with default exception handling`() {

        StepVerifier.create(defaultExceptionTask.execute("abc"))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.getOrNull()
                assertThat(value, notNullValue())
                assertThat(value?.orNull(), equalTo(3))
            }.verifyComplete()

        StepVerifier.create(defaultExceptionTask.execute(Option.fromNullable(null)))
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

        StepVerifier.create(completeTask.execute(Option.fromNullable(null)))
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

        StepVerifier.create(taskWithoutThreadPool.execute(Option.fromNullable(null)))
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

        StepVerifier.create(taskWithThreadPool.execute(Option.fromNullable(null)))
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
    fun `should create simple async task`() {
        val task = asyncTask<String, Int> {
            name("abc")
            registry(registry)
            executorOption(emptyThreadPool)
            retryOption(Option.fromNullable(retryConfig.get("abc")))
            circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
            processor { str: String -> Mono.just(Option.fromNullable(str.length)) }
        }.build()

        assertThat(task, notNullValue())

        assertThat(
            asyncTask<String, Int> {
                name("abc")
                registry(registry)
                executorOption(emptyThreadPool)
                retryOption(Option.fromNullable(retryConfig.get("abc")))
                circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
                processor { str: String -> Mono.just(Option.fromNullable(str.length)) }
                exceptionHandler { throw it }
            }.build(),
            notNullValue()
        )
    }

    @Test
    fun `should fail create`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            asyncTask<String, Int> {
                registry(registry)
                retryOption(Option.fromNullable(retryConfig.get("abc")))
                circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
                executorOption(emptyThreadPool)
                processor { str: String -> Mono.just(Option.fromNullable(str.length)) }
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            asyncTask<String, Int> {
                registry(registry)
                retryOption(Option.fromNullable(retryConfig.get("abc")))
                circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
                executorOption(emptyThreadPool)
                name("")
                processor { str: String -> Mono.just(Option.fromNullable(str.length)) }
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            asyncTask<String, Int> {
                registry(registry)
                retryOption(Option.fromNullable(retryConfig.get("abc")))
                circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
                executorOption(emptyThreadPool)
                name("  ")
                processor { str: String -> Mono.just(Option.fromNullable(str.length)) }
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            asyncTask<String, Int> {
                name("abc")
                registry(registry)
                retryOption(Option.fromNullable(retryConfig.get("abc")))
                circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
                executorOption(emptyThreadPool)
            }.build()
        }
    }
}
