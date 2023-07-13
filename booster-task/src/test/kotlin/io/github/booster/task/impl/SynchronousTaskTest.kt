package io.github.booster.task.impl

import arrow.core.Either
import io.github.booster.task.circuitBreakerConfig
import io.github.booster.task.emptyThreadPool
import io.github.booster.task.lengthExceptionThrower
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

    private val taskWithThreadPool = SynchronousTask<String, Int>(
        "lengthTask",
        threadPool,
        retryConfig.get("abc"),
        circuitBreakerConfig.get("abc"),
        registry,
        ::syncLengthFunc,
        ::lengthExceptionThrower
    )

    private val taskWithoutThreadPool = SynchronousTask<String, Int>(
        "lengthTask",
        emptyThreadPool,
        retryConfig.get("abc"),
        circuitBreakerConfig.get("abc"),
        registry,
        ::syncLengthFunc,
        ::lengthExceptionThrower
    )

    private val completeTask = SynchronousTask<String, Int>(
        "lengthTask",
        emptyThreadPool,
        retryConfig.get("test"),
        circuitBreakerConfig.get("test"),
        registry,
        ::syncLengthFunc,
        ::lengthExceptionThrower
    )

    private val defaultExceptionTask = SynchronousTask<String, Int>(
        "lengthTask",
        emptyThreadPool,
        retryConfig.get("test"),
        circuitBreakerConfig.get("test"),
        registry,
        ::syncLengthFunc,
        null
    )

    @Test
    fun `should run with default exception handling`() {

        StepVerifier.create(defaultExceptionTask.execute("abc"))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.getOrNull()
                assertThat(value, notNullValue())
                assertThat(value, equalTo(3))
            }.verifyComplete()

        StepVerifier.create(defaultExceptionTask.execute(Either.Right<String?>(null)))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.getOrNull()
                assertThat(value, notNullValue())
                assertThat(value, equalTo(0))
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
                assertThat(value, equalTo(3))
            }.verifyComplete()

        StepVerifier.create(completeTask.execute(Either.Right<String?>(null)))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.getOrNull()
                assertThat(value, notNullValue())
                assertThat(value, equalTo(0))
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
                assertThat(value, equalTo(3))
            }.verifyComplete()

        StepVerifier.create(taskWithoutThreadPool.execute(Either.Right<String?>(null)))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.getOrNull()
                assertThat(value, notNullValue())
                assertThat(value, equalTo(0))
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
                assertThat(value, equalTo(3))
            }.verifyComplete()

        StepVerifier.create(taskWithThreadPool.execute(Either.Right<String?>(null)))
            .consumeNextWith {
                assertThat(it, notNullValue())
                val value = it.getOrNull()
                assertThat(value, notNullValue())
                assertThat(value, equalTo(0))
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
        val task = syncTask<String, Int> {
            name("abc")
            registry(registry)
            executorOption(emptyThreadPool)
            retryOption(retryConfig.get("abc"))
            circuitBreakerOption(circuitBreakerConfig.get("abc"))
            processor { str: String? -> str?.length ?: 0 }
        }.build()

        assertThat(task, notNullValue())

        assertThat(
            syncTask<String, Int> {
                name("abc")
                registry(registry)
                executorOption(emptyThreadPool)
                retryOption(retryConfig.get("abc"))
                circuitBreakerOption(circuitBreakerConfig.get("abc"))
                processor { str: String? -> str?.length ?: 0 }
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
                retryOption(retryConfig.get("abc"))
                circuitBreakerOption(circuitBreakerConfig.get("abc"))
                executorOption(emptyThreadPool)
                processor { str: String? -> str?.length ?: 0 }
            }.build()
        }

        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) {
            syncTask<String, Int> {
                registry(registry)
                retryOption(retryConfig.get("abc"))
                circuitBreakerOption(circuitBreakerConfig.get("abc"))
                executorOption(emptyThreadPool)
                name("")
                processor { str: String? -> str?.length ?: 0 }
            }.build()
        }

        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) {
            syncTask<String, Int> {
                registry(registry)
                retryOption(retryConfig.get("abc"))
                circuitBreakerOption(circuitBreakerConfig.get("abc"))
                executorOption(emptyThreadPool)
                name("  ")
                processor { str: String? -> str?.length ?: 0 }
            }.build()
        }

        Assertions.assertThrows(
            IllegalArgumentException::class.java
        ) {
            syncTask<String, Int> {
                name("abc")
                registry(registry)
                retryOption(retryConfig.get("abc"))
                circuitBreakerOption(circuitBreakerConfig.get("abc"))
                executorOption(emptyThreadPool)
            }.build()
        }
    }

    @Test
    fun `should catch exception`() {

        val task = syncTask<String, Int> {
            name("abc")
            registry(registry)
            retryOption(retryConfig.get("abc"))
            circuitBreakerOption(circuitBreakerConfig.get("abc"))
            executorOption(emptyThreadPool)
            processor {
                it!!.length
            }
        }.build()

        StepVerifier.create(task.execute(null))
            .consumeNextWith {
                assertThat(it.isLeft(), equalTo(true))
                val error = it.swap().getOrNull()
                assertThat(error, notNullValue())
                assertThat(error, instanceOf(NullPointerException::class.java))
            }.verifyComplete()
    }
}
