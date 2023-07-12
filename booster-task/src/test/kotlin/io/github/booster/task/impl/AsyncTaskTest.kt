package io.github.booster.task.impl

import arrow.core.Either
import io.github.booster.task.asyncLengthFunc
import io.github.booster.task.circuitBreakerConfig
import io.github.booster.task.emptyThreadPool
import io.github.booster.task.lengthExceptionThrower
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

    private val taskWithThreadPool = AsyncTask<String, Int>(
        "lengthTask",
        threadPool,
        retryConfig.get("abc"),
        circuitBreakerConfig.get("abc"),
        registry,
        ::asyncLengthFunc,
        ::lengthExceptionThrower
    )

    private val taskWithoutThreadPool = AsyncTask<String, Int>(
        "lengthTask",
        emptyThreadPool,
        retryConfig.get("abc"),
        circuitBreakerConfig.get("abc"),
        registry,
        ::asyncLengthFunc,
        ::lengthExceptionThrower
    )

    private val completeTask = AsyncTask<String, Int>(
        "lengthTask",
        emptyThreadPool,
        retryConfig.get("test"),
        circuitBreakerConfig.get("test"),
        registry,
        ::asyncLengthFunc,
        ::lengthExceptionThrower
    )

    private val defaultExceptionTask = AsyncTask<String, Int>(
        "lengthTask",
        emptyThreadPool,
        retryConfig.get("test"),
        circuitBreakerConfig.get("test"),
        registry,
        ::asyncLengthFunc,
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
    fun `should create simple async task`() {
        val task = asyncTask<String, Int> {
            name("abc")
            registry(registry)
            executorOption(emptyThreadPool)
            retryOption(retryConfig.get("abc"))
            circuitBreakerOption(circuitBreakerConfig.get("abc"))
            processor { str: String? -> Mono.just(str?.length ?: 0) }
        }.build()

        assertThat(task, notNullValue())

        assertThat(
            asyncTask<String, Int> {
                name("abc")
                registry(registry)
                executorOption(emptyThreadPool)
                retryOption(retryConfig.get("abc"))
                circuitBreakerOption(circuitBreakerConfig.get("abc"))
                processor { str: String? -> Mono.just(str?.length ?: 0) }
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
                retryOption(retryConfig.get("abc"))
                circuitBreakerOption(circuitBreakerConfig.get("abc"))
                executorOption(emptyThreadPool)
                processor { str: String? -> Mono.just(str?.length ?: 0) }
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            asyncTask<String, Int> {
                registry(registry)
                retryOption(retryConfig.get("abc"))
                circuitBreakerOption(circuitBreakerConfig.get("abc"))
                executorOption(emptyThreadPool)
                name("")
                processor { str: String? -> Mono.just(str?.length ?: 0) }
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            asyncTask<String, Int> {
                registry(registry)
                retryOption(retryConfig.get("abc"))
                circuitBreakerOption(circuitBreakerConfig.get("abc"))
                executorOption(emptyThreadPool)
                name("  ")
                processor { str: String? -> Mono.just(str?.length ?: 0) }
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            asyncTask<String, Int> {
                name("abc")
                registry(registry)
                retryOption(retryConfig.get("abc"))
                circuitBreakerOption(circuitBreakerConfig.get("abc"))
                executorOption(emptyThreadPool)
            }.build()
        }
    }
}
