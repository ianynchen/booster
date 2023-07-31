package io.github.booster.task

import arrow.core.Either
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

internal class MonoTest {

    private fun handleException(t: Throwable): Int {
        return if (t is IllegalArgumentException) {
            throw t
        } else {
            0
        }
    }

    private fun processor(value: Either<Throwable, Int?>): Mono<Int> {
        return if (value.isRight()) {
            Mono.just(value.getOrNull()!!)
        } else {
            Mono.fromSupplier { handleException(value.swap().getOrNull()!!) }
        }
    }

    @Test
    fun `should handle error`() {
        StepVerifier.create(processor(Either.Left(IllegalStateException())))
            .consumeNextWith {
                assertThat(it, equalTo(0))
            }.verifyComplete()

        StepVerifier.create(processor(Either.Left(IllegalArgumentException())))
            .expectError(IllegalArgumentException::class.java)
            .verify()
    }
}
