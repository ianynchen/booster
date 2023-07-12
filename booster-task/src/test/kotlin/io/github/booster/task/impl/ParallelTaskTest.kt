package io.github.booster.task.impl

import arrow.core.Either
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
                syncTask<String?, Int> {
                    name("length")
                    registry(io.github.booster.task.registry)
                    retryOption(retryConfig.get("abc"))
                    circuitBreakerOption(circuitBreakerConfig.get("abc"))
                    executorOption(emptyThreadPool)
                    processor {
                        it?.length ?: 0
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
        val task = parallelTask<String?, Int> {
            name("parallel")
            registry(io.github.booster.task.registry)
            requestErrorHandler {
                if (it is IllegalArgumentException) listOf() else listOf(1, 1)
            }
            task(
                syncTask<String?, Int> {
                    name("length")
                    registry(io.github.booster.task.registry)
                    retryOption(retryConfig.get("abc"))
                    circuitBreakerOption(circuitBreakerConfig.get("abc"))
                    executorOption(emptyThreadPool)
                    processor {
                        it?.length ?: 0
                    }
                }.build()
            )
        }.build()

        StepVerifier.create(task.execute(Either.Left(IllegalArgumentException())))
            .consumeNextWith {
                assertThat(it.isRight(), `is`(true))
                assertThat(it.getOrNull()?.size ?: 0, `is`(0))
            }.verifyComplete()

        StepVerifier.create(task.execute(Either.Left(IllegalStateException())))
            .consumeNextWith {
                assertThat(it.isRight(), `is`(true))
                assertThat(it.getOrNull()?.size ?: 0, `is`(2))
                assertThat(it.getOrNull()!!, containsInAnyOrder(1, 1))
            }.verifyComplete()
    }

    @Test
    @Suppress("UseRequire")
    fun `should calculate length`() {
        val task = parallelTask<String?, Int> {
            name("parallel")
            registry(io.github.booster.task.registry)
            executorOption(threadPool)
            task(
                syncTask<String?, Int> {
                    name("length")
                    registry(io.github.booster.task.registry)
                    retryOption(retryConfig.get("abc"))
                    circuitBreakerOption(circuitBreakerConfig.get("abc"))
                    executorOption(emptyThreadPool)
                    processor {
                        it!!.length
                    }
                }.build()
            )
            aggregator {
                if (!isAllRight(it)) {
                    throw IllegalArgumentException("not all right values")
                } else {
                    it.map { either -> either.getOrNull() }
                }
            }
        }.build()

        StepVerifier.create(task.execute(listOf("a", "ab", "abc")))
            .consumeNextWith {
                assertThat(it.isRight(), `is`(true))
                assertThat(it.getOrNull()?.size ?: 0, `is`(3))
                assertThat(it.getOrNull()!!, containsInAnyOrder(1, 2, 3))
            }.verifyComplete()

        StepVerifier.create(task.execute(listOf("a", null, "abc")))
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
                    retryOption(retryConfig.get("abc"))
                    circuitBreakerOption(circuitBreakerConfig.get("abc"))
                    executorOption(emptyThreadPool)
                    processor {
                        it!!.length
                    }
                }.build()
            )
        }.build()

        StepVerifier.create(task.execute(listOf(null, null, null, "abcd")))
            .consumeNextWith {
                assertThat(it.isRight(), `is`(true))
                assertThat(it.getOrElse { listOf() }, hasSize(1))
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
    fun `should run with custom aggregator`() {
        val task = parallelTask {
            name("parallel")
            registry(io.github.booster.task.registry)
            requestErrorHandler { throw it }
            task(
                syncTask<String?, Int> {
                    name("length")
                    registry(io.github.booster.task.registry)
                    retryOption(retryConfig.get("abc"))
                    circuitBreakerOption(circuitBreakerConfig.get("abc"))
                    executorOption(emptyThreadPool)
                    processor {
                        it?.length ?: 0
                    }
                }.build()
            )
        }.build()
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
                syncTask<String?, Int> {
                    name("length")
                    registry(io.github.booster.task.registry)
                    retryOption(retryConfig.get("abc"))
                    circuitBreakerOption(circuitBreakerConfig.get("abc"))
                    executorOption(emptyThreadPool)
                    processor {
                        it?.length ?: 0
                    }
                }.build()
            )
        }.build()

        assertThat(task, notNullValue())

        assertThat(
            parallelTask {
                registry(io.github.booster.task.registry)
                task(
                    syncTask<String?, Int> {
                        name("length")
                        registry(io.github.booster.task.registry)
                        retryOption(retryConfig.get("abc"))
                        circuitBreakerOption(circuitBreakerConfig.get("abc"))
                        executorOption(emptyThreadPool)
                        processor {
                            it?.length ?: 0
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
                    syncTask<String?, Int> {
                        name("length")
                        registry(io.github.booster.task.registry)
                        retryOption(retryConfig.get("abc"))
                        circuitBreakerOption(circuitBreakerConfig.get("abc"))
                        executorOption(emptyThreadPool)
                        processor {
                            it?.length ?: 0
                        }
                    }.build()
                )
            }.build(),
            notNullValue()
        )
    }
}
