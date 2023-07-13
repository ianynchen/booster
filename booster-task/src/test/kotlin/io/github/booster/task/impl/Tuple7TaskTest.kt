package io.github.booster.task.impl

import arrow.core.Either
import io.github.booster.task.lengthTask
import io.github.booster.task.stringTask
import io.vavr.Tuple
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

internal class Tuple7TaskTest {

    @Suppress("LongMethod")
    @Test
    fun `should fail to create task`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            tuple7Task {
                firstTask(lengthTask)
                secondTask(stringTask)
                thirdTask(lengthTask)
                fourthTask(stringTask)
                fifthTask(lengthTask)
                sixthTask(stringTask)
                seventhTask(lengthTask)
                aggregator {
                        either1, either2, either3, either4,
                        either5, either6, either7
                    ->
                    Tuple.of(
                        either1.getOrNull(),
                        either2.getOrNull(),
                        either3.getOrNull(),
                        either4.getOrNull(),
                        either5.getOrNull(),
                        either6.getOrNull(),
                        either7.getOrNull()
                    )
                }
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            tuple7Task {
                name("")
                firstTask(lengthTask)
                secondTask(stringTask)
                thirdTask(lengthTask)
                fourthTask(stringTask)
                fifthTask(lengthTask)
                sixthTask(stringTask)
                seventhTask(lengthTask)
                aggregator {
                        either1, either2, either3, either4,
                        either5, either6, either7
                    ->
                    Tuple.of(
                        either1.getOrNull(),
                        either2.getOrNull(),
                        either3.getOrNull(),
                        either4.getOrNull(),
                        either5.getOrNull(),
                        either6.getOrNull(),
                        either7.getOrNull()
                    )
                }
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            tuple7Task {
                name(" ")
                firstTask(lengthTask)
                secondTask(stringTask)
                thirdTask(lengthTask)
                fourthTask(stringTask)
                fifthTask(lengthTask)
                sixthTask(stringTask)
                seventhTask(lengthTask)
                aggregator {
                        either1, either2, either3, either4,
                        either5, either6, either7
                    ->
                    Tuple.of(
                        either1.getOrNull(),
                        either2.getOrNull(),
                        either3.getOrNull(),
                        either4.getOrNull(),
                        either5.getOrNull(),
                        either6.getOrNull(),
                        either7.getOrNull()
                    )
                }
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            tuple7Task<String?, Int, Int?, String, String?, Int, Int?, String,
                    String?, Int, Int?, String, String?, Int> {
                name("abc")
                secondTask(stringTask)
                thirdTask(lengthTask)
                fourthTask(stringTask)
                fifthTask(lengthTask)
                sixthTask(stringTask)
                seventhTask(lengthTask)
                aggregator {
                        either1, either2, either3, either4,
                        either5, either6, either7
                    ->
                    Tuple.of(
                        either1.getOrNull(),
                        either2.getOrNull(),
                        either3.getOrNull(),
                        either4.getOrNull(),
                        either5.getOrNull(),
                        either6.getOrNull(),
                        either7.getOrNull()
                    )
                }
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            tuple7Task<String?, Int, Int?, String, String?, Int, Int?, String,
                    String?, Int, Int?, String, String?, Int> {
                name("abc")
                firstTask(lengthTask)
                thirdTask(lengthTask)
                fourthTask(stringTask)
                fifthTask(lengthTask)
                sixthTask(stringTask)
                seventhTask(lengthTask)
                aggregator {
                        either1, either2, either3, either4,
                        either5, either6, either7
                    ->
                    Tuple.of(
                        either1.getOrNull(),
                        either2.getOrNull(),
                        either3.getOrNull(),
                        either4.getOrNull(),
                        either5.getOrNull(),
                        either6.getOrNull(),
                        either7.getOrNull()
                    )
                }
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            tuple7Task<String?, Int, Int?, String, String?, Int, Int?, String,
                    String?, Int, Int?, String, String?, Int> {
                name("abc")
                firstTask(lengthTask)
                secondTask(stringTask)
                fourthTask(stringTask)
                fifthTask(lengthTask)
                sixthTask(stringTask)
                seventhTask(lengthTask)
                aggregator {
                        either1, either2, either3, either4,
                        either5, either6, either7
                    ->
                    Tuple.of(
                        either1.getOrNull(),
                        either2.getOrNull(),
                        either3.getOrNull(),
                        either4.getOrNull(),
                        either5.getOrNull(),
                        either6.getOrNull(),
                        either7.getOrNull()
                    )
                }
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            tuple7Task<String?, Int, Int?, String, String?, Int, Int?, String,
                    String?, Int, Int?, String, String?, Int> {
                name("abc")
                firstTask(lengthTask)
                secondTask(stringTask)
                thirdTask(lengthTask)
                fifthTask(lengthTask)
                sixthTask(stringTask)
                seventhTask(lengthTask)
                aggregator {
                        either1, either2, either3, either4,
                        either5, either6, either7
                    ->
                    Tuple.of(
                        either1.getOrNull(),
                        either2.getOrNull(),
                        either3.getOrNull(),
                        either4.getOrNull(),
                        either5.getOrNull(),
                        either6.getOrNull(),
                        either7.getOrNull()
                    )
                }
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            tuple7Task<String?, Int, Int?, String, String?, Int, Int?, String,
                    String?, Int, Int?, String, String?, Int> {
                name("abc")
                firstTask(lengthTask)
                secondTask(stringTask)
                thirdTask(lengthTask)
                fourthTask(stringTask)
                sixthTask(stringTask)
                seventhTask(lengthTask)
                aggregator {
                        either1, either2, either3, either4,
                        either5, either6, either7
                    ->
                    Tuple.of(
                        either1.getOrNull(),
                        either2.getOrNull(),
                        either3.getOrNull(),
                        either4.getOrNull(),
                        either5.getOrNull(),
                        either6.getOrNull(),
                        either7.getOrNull()
                    )
                }
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            tuple7Task<String?, Int, Int?, String, String?, Int, Int?, String,
                    String?, Int, Int?, String, String?, Int> {
                name("abc")
                firstTask(lengthTask)
                secondTask(stringTask)
                thirdTask(lengthTask)
                fourthTask(stringTask)
                fifthTask(lengthTask)
                seventhTask(lengthTask)
                aggregator {
                        either1, either2, either3, either4,
                        either5, either6, either7
                    ->
                    Tuple.of(
                        either1.getOrNull(),
                        either2.getOrNull(),
                        either3.getOrNull(),
                        either4.getOrNull(),
                        either5.getOrNull(),
                        either6.getOrNull(),
                        either7.getOrNull()
                    )
                }
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            tuple7Task<String?, Int, Int?, String, String?, Int, Int?, String,
                    String?, Int, Int?, String, String?, Int> {
                name("abc")
                firstTask(lengthTask)
                secondTask(stringTask)
                thirdTask(lengthTask)
                fourthTask(stringTask)
                fifthTask(lengthTask)
                sixthTask(stringTask)
                aggregator {
                        either1, either2, either3, either4,
                        either5, either6, either7
                    ->
                    Tuple.of(
                        either1.getOrNull(),
                        either2.getOrNull(),
                        either3.getOrNull(),
                        either4.getOrNull(),
                        either5.getOrNull(),
                        either6.getOrNull(),
                        either7.getOrNull()
                    )
                }
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            tuple7Task {
                name("abc")
                firstTask(lengthTask)
                secondTask(stringTask)
                thirdTask(lengthTask)
                fourthTask(stringTask)
                fifthTask(lengthTask)
                sixthTask(stringTask)
                seventhTask(lengthTask)
            }.build()
        }
    }

    @Test
    fun `should create task`() {

        assertThat(
            tuple7Task {
                name("abc")
                firstTask(lengthTask)
                secondTask(stringTask)
                thirdTask(lengthTask)
                fourthTask(stringTask)
                fifthTask(lengthTask)
                sixthTask(stringTask)
                seventhTask(lengthTask)
                registry(io.github.booster.task.registry)
                aggregator {
                        either1, either2, either3, either4,
                        either5, either6, either7
                    ->
                    Tuple.of(
                        either1.getOrNull(),
                        either2.getOrNull(),
                        either3.getOrNull(),
                        either4.getOrNull(),
                        either5.getOrNull(),
                        either6.getOrNull(),
                        either7.getOrNull()
                    )
                }
            }.build(),
            notNullValue()
        )
    }

    @Suppress("LongMethod")
    @Test
    fun `should execute normal flow`() {
        val task = tuple7Task {
            name("abc")
            firstTask(lengthTask)
            secondTask(stringTask)
            thirdTask(lengthTask)
            fourthTask(stringTask)
            fifthTask(lengthTask)
            sixthTask(stringTask)
            seventhTask(lengthTask)
            registry(io.github.booster.task.registry)
            aggregator {
                    either1, either2, either3, either4,
                    either5, either6, either7
                ->
                Tuple.of(
                    either1.getOrNull(),
                    either2.getOrNull(),
                    either3.getOrNull(),
                    either4.getOrNull(),
                    either5.getOrNull(),
                    either6.getOrNull(),
                    either7.getOrNull()
                )
            }
        }.build()

        val response = task.execute(
            Either.Right(
                Tuple.of("abc", 12, "abcd", 123,
            "abcde", 1234, "abcdef")))
        StepVerifier.create(response)
            .consumeNextWith {
                assertThat(it, notNullValue())
                assertThat(it.isRight(), `is`(true))
                assertThat(it.getOrNull()?._1(), equalTo(3))
                assertThat(it.getOrNull()?._2(), equalTo("12"))
                assertThat(it.getOrNull()?._3(), equalTo(4))
                assertThat(it.getOrNull()?._4(), equalTo("123"))
                assertThat(it.getOrNull()?._5(), equalTo(5))
                assertThat(it.getOrNull()?._6(), equalTo("1234"))
                assertThat(it.getOrNull()?._7(), equalTo(6))
            }.verifyComplete()

        val response2 = task.execute(
            Either.Right(
                Tuple.of(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                )
            )
        )
        StepVerifier.create(response2)
            .consumeNextWith {
                assertThat(it, notNullValue())
                assertThat(it.isRight(), `is`(true))
                assertThat(it.getOrNull()?._1(), equalTo(0))
                assertThat(it.getOrNull()?._2(), equalTo(""))
                assertThat(it.getOrNull()?._3(), equalTo(0))
                assertThat(it.getOrNull()?._4(), equalTo(""))
                assertThat(it.getOrNull()?._5(), equalTo(0))
                assertThat(it.getOrNull()?._6(), equalTo(""))
                assertThat(it.getOrNull()?._7(), equalTo(0))
            }.verifyComplete()

        val response3 = task.execute(Either.Right(null))
        StepVerifier.create(response3)
            .consumeNextWith {
                assertThat(it, notNullValue())
                assertThat(it.isRight(), `is`(true))
                assertThat(it.getOrNull()?._1(), equalTo(0))
                assertThat(it.getOrNull()?._2(), equalTo(""))
                assertThat(it.getOrNull()?._3(), equalTo(0))
                assertThat(it.getOrNull()?._4(), equalTo(""))
                assertThat(it.getOrNull()?._5(), equalTo(0))
                assertThat(it.getOrNull()?._6(), equalTo(""))
                assertThat(it.getOrNull()?._7(), equalTo(0))
            }.verifyComplete()
    }

    @Test
    fun `should handle request exception with default`() {
        val task = tuple7Task {
            name("abc")
            firstTask(lengthTask)
            secondTask(stringTask)
            thirdTask(lengthTask)
            fourthTask(stringTask)
            fifthTask(lengthTask)
            sixthTask(stringTask)
            seventhTask(lengthTask)
            registry(io.github.booster.task.registry)
            aggregator {
                    either1, either2, either3, either4,
                    either5, either6, either7
                ->
                Tuple.of(
                    either1.getOrNull(),
                    either2.getOrNull(),
                    either3.getOrNull(),
                    either4.getOrNull(),
                    either5.getOrNull(),
                    either6.getOrNull(),
                    either7.getOrNull()
                )
            }
        }.build()

        val response = task.execute(Either.Left(IllegalArgumentException()))
        StepVerifier.create(response)
            .consumeNextWith {
                assertThat(it, notNullValue())
                assertThat(it.isLeft(), `is`(true))
                assertThat(it.swap().getOrNull(), notNullValue())
                assertThat(
                    it.swap().getOrNull(),
                    instanceOf(IllegalArgumentException::class.java)
                )
            }.verifyComplete()
    }

    @Test
    fun `should handle exception with custom handler`() {
        val task = tuple7Task {
            name("abc")
            exceptionHandler {
                throw IllegalStateException(it)
            }
            firstTask(lengthTask)
            secondTask(stringTask)
            thirdTask(lengthTask)
            fourthTask(stringTask)
            fifthTask(lengthTask)
            sixthTask(stringTask)
            seventhTask(lengthTask)
            registry(io.github.booster.task.registry)
            aggregator {
                    either1, either2, either3, either4,
                    either5, either6, either7
                ->
                Tuple.of(
                    either1.getOrNull(),
                    either2.getOrNull(),
                    either3.getOrNull(),
                    either4.getOrNull(),
                    either5.getOrNull(),
                    either6.getOrNull(),
                    either7.getOrNull()
                )
            }
        }.build()

        val response = task.execute(Either.Left(IllegalArgumentException()))
        StepVerifier.create(response)
            .consumeNextWith {
                assertThat(it, notNullValue())
                assertThat(it.isLeft(), `is`(true))
                assertThat(it.swap().getOrNull(), notNullValue())
                assertThat(it.swap().getOrNull(), instanceOf(IllegalStateException::class.java))

                val exception = it.swap().getOrNull()!! as IllegalStateException
                assertThat(
                    exception.cause,
                    notNullValue()
                )
                assertThat(
                    exception.cause!!,
                    instanceOf(IllegalArgumentException::class.java)
                )
            }.verifyComplete()
    }
}