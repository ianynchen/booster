package io.github.booster.task.impl

import io.github.booster.task.circuitBreakerConfig
import io.github.booster.task.retryConfig
import io.github.booster.task.threadPool
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

internal class SequentialTaskTest {

    private val task = sequentialTask {
        name("abc")
        registry(io.github.booster.task.registry)
        firstTask(
            syncTask<String, Int?> {
                name("length")
                registry(io.github.booster.task.registry)
                retryOption(retryConfig.get("abc"))
                circuitBreakerOption(circuitBreakerConfig.get("abc"))
                executorOption(threadPool)
                processor {
                    it!!.length
                }
            }.build()
        )
        secondTask(
            syncTask<Int, String> {
                name("str")
                registry(io.github.booster.task.registry)
                retryOption(retryConfig.get("abc"))
                circuitBreakerOption(circuitBreakerConfig.get("abc"))
                executorOption(threadPool)
                processor {
                    it.toString()
                }
            }.build()
        )
    }.build();

    @Test
    fun `should create sequential task`() {
        val task = sequentialTask {
            name("test")
            registry(io.github.booster.task.registry)
            firstTask(
                syncTask<String, Int?> {
                    name("length")
                    registry(io.github.booster.task.registry)
                    retryOption(retryConfig.get("abc"))
                    circuitBreakerOption(circuitBreakerConfig.get("abc"))
                    executorOption(threadPool)
                    processor {
                        it?.length ?: 0
                    }
                }.build()
            )
            secondTask(
                syncTask<Int, String> {
                    name("str")
                    registry(io.github.booster.task.registry)
                    retryOption(retryConfig.get("abc"))
                    circuitBreakerOption(circuitBreakerConfig.get("abc"))
                    executorOption(threadPool)
                    processor {
                        it.toString()
                    }
                }.build()
            )
        }.build()

        assertThat(task, CoreMatchers.notNullValue())
    }

    @Test
    fun `should fail create missing tasks`() {

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            sequentialTask<String, Int, String> {
                name("test")
                registry(io.github.booster.task.registry)
                secondTask(
                    syncTask<Int, String> {
                        name("str")
                        registry(io.github.booster.task.registry)
                        retryOption(retryConfig.get("abc"))
                        circuitBreakerOption(circuitBreakerConfig.get("abc"))
                        executorOption(threadPool)
                        processor {
                            it.toString()
                        }
                    }.build()
                )
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            sequentialTask<String, Int, String> {
                name("test")
                registry(io.github.booster.task.registry)
                firstTask(
                    syncTask<String, Int?> {
                        name("length")
                        registry(io.github.booster.task.registry)
                        retryOption(retryConfig.get("abc"))
                        circuitBreakerOption(circuitBreakerConfig.get("abc"))
                        executorOption(threadPool)
                        processor {
                            it?.length ?: 0
                        }
                    }.build()
                )
            }.build()
        }
    }

    @Test
    fun `should create without name`() {
        assertThat(
            sequentialTask {
                registry(io.github.booster.task.registry)
                firstTask(
                    syncTask<String, Int?> {
                        name("length")
                        registry(io.github.booster.task.registry)
                        retryOption(retryConfig.get("abc"))
                        circuitBreakerOption(circuitBreakerConfig.get("abc"))
                        executorOption(threadPool)
                        processor {
                            it?.length ?: 0
                        }
                    }.build()
                )
                secondTask(
                    syncTask<Int, String> {
                        name("str")
                        registry(io.github.booster.task.registry)
                        retryOption(retryConfig.get("abc"))
                        circuitBreakerOption(circuitBreakerConfig.get("abc"))
                        executorOption(threadPool)
                        processor {
                            it.toString()
                        }
                    }.build()
                )
            }.build(),
            notNullValue()
        )
    }

    @Test
    fun `should execute`() {

        StepVerifier.create(task.execute("abc"))
            .consumeNextWith {
                assertThat(it.isRight(), `is`(true))
                assertThat(it.getOrNull(), equalTo("3"))
            }.verifyComplete()
    }

    @Test
    fun `should create with name`() {

        assertThat(
            this.task,
            notNullValue()
        )
    }
}
