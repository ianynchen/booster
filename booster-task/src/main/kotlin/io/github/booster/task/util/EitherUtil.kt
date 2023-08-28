package io.github.booster.task.util

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import io.github.booster.task.Maybe
import reactor.core.publisher.Mono

/**
 * Wraps a nullable value into <b>Either<Throwable, Option<T>></b>
 */
fun <T> toEither(value: T?): Maybe<T> =
    Either.Right(Option.fromNullable(value))

/**
 * Wraps a nullable value into <b>Mono<Either<Throwable, Option<T>>></b>
 */
fun <T> toMonoEither(value: T?): Mono<Maybe<T>> =
    Mono.just(toEither(value))

fun <T> Maybe<T>.extractValue(): Option<T> =
    this.getOrElse {
        Option.fromNullable(null)
    }
