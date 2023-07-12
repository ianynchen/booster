package io.github.booster.task.util

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

internal class EitherUtilKtTest {

    @Test
    fun `should create either`() {
        assertThat(toEither(1), notNullValue())

        val either = toEither<Int>(null)
        assertThat(either, notNullValue())
        assertThat(either.isRight(), `is`(true))
        assertThat(either.getOrNull(), nullValue())
    }

    @Test
    fun `should create mono`() {
        assertThat(toMonoEither(1), notNullValue())
        assertThat(toMonoEither<Int>(null), notNullValue())
    }
}
