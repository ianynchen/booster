package io.github.booster.task.util

import arrow.core.Either
import reactor.core.publisher.Mono

fun <T> toEither(value: T?): Either<Throwable, T?> =
    Either.Right(value)

fun <T> toMonoEither(value: T?): Mono<Either<Throwable, T?>> =
    Mono.just(toEither(value))
