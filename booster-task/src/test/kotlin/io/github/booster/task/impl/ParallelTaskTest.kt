package io.github.booster.task.impl

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import io.github.booster.task.circuitBreakerConfig
import io.github.booster.task.emptyThreadPool
import io.github.booster.task.retryConfig
import io.github.booster.task.threadPool
import io.github.booster.task.util.isAllRight
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

internal class ParallelTaskTest {

    @Test
    fun `should use default exception`() {
        val task = parallelTask {
            name("parallel")
            registry(io.github.booster.task.registry)
            task(
                syncTask<String, Int> {
                    name("length")
                    registry(io.github.booster.task.registry)
                    retryOption(Option.fromNullable(retryConfig.get("abc")))
                    circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
                    executorOption(emptyThreadPool)
                    processor {
                        Option.fromNullable(it.length)
                    }
                }.build()
            )
        }.build()

        val response = task.execute(Either.Left(IllegalArgumentException()))
        StepVerifier.create(response)
            .consumeNextWith {
                assertThat(it.isLeft(), `is`(true))
                assertThat(it.swap().getOrNull()!!, instanceOf(IllegalArgumentException::class.java))
            }.verifyComplete()
    }

    @Test
    fun `should use custom request exception handler`() {
        val task = parallelTask {
            name("parallel")
            registry(io.github.booster.task.registry)
            requestErrorHandler {
                if (it is IllegalArgumentException) Option.fromNullable(listOf())
                else Option.fromNullable(listOf(1, 1))
            }
            task(
                syncTask<String, Int> {
                    name("length")
                    registry(io.github.booster.task.registry)
                    retryOption(Option.fromNullable(retryConfig.get("abc")))
                    circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
                    executorOption(emptyThreadPool)
                    processor {
                        Option.fromNullable(it.length)
                    }
                }.build()
            )
        }.build()

        StepVerifier.create(task.execute(Either.Left(IllegalArgumentException())))
            .consumeNextWith {
                assertThat(it.isRight(), `is`(true))
                assertThat(it.getOrNull()?.isDefined(), `is`(true))
                val list = it.getOrNull()?.orNull()
                assertThat(list, notNullValue())
                assertThat(list, hasSize(0))
            }.verifyComplete()

        StepVerifier.create(task.execute(Either.Left(IllegalStateException())))
            .consumeNextWith {
                assertThat(it.isRight(), `is`(true))
                assertThat(it.getOrNull()?.isDefined(), `is`(true))
                val list = it.getOrNull()?.orNull()
                assertThat(list, notNullValue())
                assertThat(list, hasSize(2))
                assertThat(list, containsInAnyOrder(1, 1))
            }.verifyComplete()
    }

    @Test
    @Suppress("UseRequire")
    fun `should calculate length`() {
        val task = parallelTask {
            name("parallel")
            registry(io.github.booster.task.registry)
            executorOption(threadPool)
            task(
                syncTask<String?, Int> {
                    name("length")
                    registry(io.github.booster.task.registry)
                    retryOption(Option.fromNullable(retryConfig.get("abc")))
                    circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
                    defaultRequestHandler { throw IllegalStateException("error") }
                    executorOption(emptyThreadPool)
                    processor {
                        Option.fromNullable(it?.length)
                    }
                }.build()
            )
            aggregator {
                if (!isAllRight(it)) {
                    throw IllegalArgumentException("not all right values")
                } else {
                    Option.fromNullable(
                        it.mapNotNull { either -> either.getOrNull()?.orNull() }
                    )
                }
            }
        }.build()

        StepVerifier.create(task.execute(listOf("a", "ab", "abc")))
            .consumeNextWith {
                assertThat(it.isRight(), `is`(true))
                assertThat(it.getOrNull()?.isDefined(), `is`(true))

                val list = it.getOrNull()?.orNull()
                assertThat(list, notNullValue())
                assertThat(list, hasSize(3))
                assertThat(list, containsInAnyOrder(1, 2, 3))
            }.verifyComplete()

        StepVerifier.create(task.execute(Option.fromNullable(listOf("a", null, "abc"))))
            .consumeNextWith {
                assertThat(it.isRight(), `is`(false))
                assertThat(it.swap().getOrNull(), notNullValue())
                assertThat(it.swap().getOrNull(), instanceOf(IllegalArgumentException::class.java))
            }.verifyComplete()
    }

    @Test
    fun `should run with default aggregator`() {
        val task = parallelTask {
            name("parallel")
            registry(io.github.booster.task.registry)
            requestErrorHandler { throw it }
            task(
                syncTask<String?, Int> {
                    name("length")
                    registry(io.github.booster.task.registry)
                    retryOption(Option.fromNullable(retryConfig.get("abc")))
                    circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
                    executorOption(emptyThreadPool)
                    defaultRequestHandler { throw NullPointerException() }
                    processor {
                        Option.fromNullable(it?.length ?: 0)
                    }
                }.build()
            )
        }.build()

        StepVerifier.create(task.execute(Option.fromNullable(listOf(null, null, null, "abcd"))))
            .consumeNextWith {
                assertThat(it.isRight(), `is`(true))
                val value = it.getOrNull()?.getOrElse { listOf() }
                assertThat(value, notNullValue())
                assertThat(value, hasSize(1))
            }.verifyComplete()

        StepVerifier.create(task.execute(listOf(null, null, null, null)))
            .consumeNextWith {
                assertThat(it.isRight(), `is`(false))
                assertThat(
                    it.swap().getOrElse { IllegalStateException() },
                    instanceOf(IllegalArgumentException::class.java)
                )
            }.verifyComplete()
    }

    @Test
    fun `should fail create task`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            parallelTask<String, Int> {
                name("parallel")
                registry(io.github.booster.task.registry)
                requestErrorHandler { throw it }
            }.build()
        }
    }

    @Test
    fun `should create parallel task`() {
        val task = parallelTask {
            name("parallel")
            registry(io.github.booster.task.registry)
            requestErrorHandler { throw it }
            task(
                syncTask<String, Int> {
                    name("length")
                    registry(io.github.booster.task.registry)
                    retryOption(Option.fromNullable(retryConfig.get("abc")))
                    circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
                    executorOption(emptyThreadPool)
                    processor {
                        Option.fromNullable(it.length)
                    }
                }.build()
            )
        }.build()

        assertThat(task, notNullValue())

        assertThat(
            parallelTask {
                registry(io.github.booster.task.registry)
                task(
                    syncTask<String, Int> {
                        name("length")
                        registry(io.github.booster.task.registry)
                        retryOption(Option.fromNullable(retryConfig.get("abc")))
                        circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
                        executorOption(emptyThreadPool)
                        processor {
                            Option.fromNullable(it.length)
                        }
                    }.build()
                )
            }.build(),
            notNullValue()
        )

        assertThat(
            parallelTask {
                name(null)
                registry(io.github.booster.task.registry)
                task(
                    syncTask<String, Int> {
                        name("length")
                        registry(io.github.booster.task.registry)
                        retryOption(Option.fromNullable(retryConfig.get("abc")))
                        circuitBreakerOption(Option.fromNullable(circuitBreakerConfig.get("abc")))
                        executorOption(emptyThreadPool)
                        processor {
                            Option.fromNullable(it.length)
                        }
                    }.build()
                )
            }.build(),
            notNullValue()
        )
    }
}
