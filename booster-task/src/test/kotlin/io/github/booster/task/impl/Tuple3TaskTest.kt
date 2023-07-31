package io.github.booster.task.impl

import arrow.core.Either
import arrow.core.Option
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

internal class Tuple3TaskTest {

    @Suppress("LongMethod")
    @Test
    fun `should fail to create task`() {
        assertThrows(
            IllegalArgumentException::class.java
        ) {
            tuple3Task {
                firstTask(lengthTask)
                secondTask(stringTask)
                thirdTask(lengthTask)
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            tuple3Task {
                name("")
                firstTask(lengthTask)
                secondTask(stringTask)
                thirdTask(lengthTask)
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            tuple3Task {
                name(" ")
                firstTask(lengthTask)
                secondTask(stringTask)
                thirdTask(lengthTask)
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            tuple3Task<String, Int, Int, String, String, Int> {
                name("abc")
                secondTask(stringTask)
                thirdTask(lengthTask)
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            tuple3Task<String, Int, Int, String, String, Int> {
                name("abc")
                firstTask(lengthTask)
                thirdTask(lengthTask)
            }.build()
        }

        assertThrows(
            IllegalArgumentException::class.java
        ) {
            tuple3Task<String, Int, Int, String, String, Int> {
                name("abc")
                firstTask(lengthTask)
                secondTask(stringTask)
            }.build()
        }
    }

    @Test
    fun `should create task`() {

        assertThat(
            tuple3Task {
                name("abc")
                firstTask(lengthTask)
                secondTask(stringTask)
                thirdTask(lengthTask)
                registry(io.github.booster.task.registry)
            }.build(),
            notNullValue()
        )
    }

    @Suppress("LongMethod")
    @Test
    fun `should execute normal flow`() {
        val task = tuple3Task {
            name("tuple")
            firstTask(lengthTask)
            secondTask(stringTask)
            thirdTask(lengthTask)
            registry(io.github.booster.task.registry)
        }.build()

        val response = task.execute(
            Tuple.of(
                Option.fromNullable("abc"),
                Option.fromNullable(12),
                Option.fromNullable("abcd")
            )
        )
        StepVerifier.create(response)
            .consumeNextWith {
                assertThat(it, notNullValue())
                assertThat(it.isRight(), `is`(true))

                val tupleWithErrorOption = it.getOrNull()
                assertThat(tupleWithErrorOption, notNullValue())
                assertThat(tupleWithErrorOption!!.isDefined(), `is`(true))

                val tupleWithError = tupleWithErrorOption.orNull()
                assertThat(tupleWithError, notNullValue())

                val a = tupleWithError?._1()
                val b = tupleWithError?._2()
                val c = tupleWithError?._3()

                assertThat(a, notNullValue())
                assertThat(b, notNullValue())
                assertThat(c, notNullValue())
                assertThat(a?.isRight(), `is`(true))
                assertThat(b?.isRight(), `is`(true))
                assertThat(c?.isRight(), `is`(true))

                assertThat(a?.getOrNull()?.orNull(), equalTo(3))
                assertThat(b?.getOrNull()?.orNull(), equalTo("12"))
                assertThat(c?.getOrNull()?.orNull(), equalTo(4))
            }.verifyComplete()

        val response2 = task.execute(
            Tuple.of(
                Option.fromNullable(null),
                Option.fromNullable(null),
                Option.fromNullable(null)
            )
        )
        StepVerifier.create(response2)
            .consumeNextWith {
                assertThat(it, notNullValue())
                assertThat(it.isRight(), `is`(true))

                val tupleWithErrorOption = it.getOrNull()
                assertThat(tupleWithErrorOption, notNullValue())
                assertThat(tupleWithErrorOption!!.isDefined(), `is`(true))

                val tupleWithError = tupleWithErrorOption.orNull()
                assertThat(tupleWithError, notNullValue())

                val a = tupleWithError?._1()
                val b = tupleWithError?._2()
                val c = tupleWithError?._3()

                assertThat(a, notNullValue())
                assertThat(b, notNullValue())
                assertThat(c, notNullValue())
                assertThat(a?.isRight(), `is`(true))
                assertThat(b?.isRight(), `is`(true))
                assertThat(c?.isRight(), `is`(true))

                assertThat(a?.getOrNull()?.orNull(), equalTo(0))
                assertThat(b?.getOrNull()?.orNull(), equalTo(""))
                assertThat(c?.getOrNull()?.orNull(), equalTo(0))
            }.verifyComplete()

        val response3 = task.execute(Option.fromNullable(null))
        StepVerifier.create(response3)
            .consumeNextWith {
                assertThat(it, notNullValue())
                assertThat(it.isRight(), `is`(true))

                val tupleWithErrorOption = it.getOrNull()
                assertThat(tupleWithErrorOption, notNullValue())
                assertThat(tupleWithErrorOption!!.isDefined(), `is`(true))

                val tupleWithError = tupleWithErrorOption.orNull()
                assertThat(tupleWithError, notNullValue())

                val a = tupleWithError?._1()
                val b = tupleWithError?._2()
                val c = tupleWithError?._3()

                assertThat(a, notNullValue())
                assertThat(b, notNullValue())
                assertThat(c, notNullValue())
                assertThat(a?.isRight(), `is`(true))
                assertThat(b?.isRight(), `is`(true))
                assertThat(c?.isRight(), `is`(true))

                assertThat(a?.getOrNull()?.orNull(), equalTo(0))
                assertThat(b?.getOrNull()?.orNull(), equalTo(""))
                assertThat(c?.getOrNull()?.orNull(), equalTo(0))
            }.verifyComplete()
    }

    @Test
    fun `should handle request exception with default`() {
        val task = tuple3Task {
            name("tuple")
            firstTask(lengthTask)
            secondTask(stringTask)
            thirdTask(lengthTask)
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
        val task = tuple3Task {
            name("tuple")
            exceptionHandler {
                throw IllegalStateException(it)
            }
            firstTask(lengthTask)
            secondTask(stringTask)
            thirdTask(lengthTask)
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
