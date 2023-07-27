package io.github.booster.task.util

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.orElse
import io.github.booster.task.DataWithError
import io.github.booster.task.impl.OptionTuple2
import io.vavr.Tuple.of
import io.vavr.Tuple2

fun <T> isAnyRight(values: List<Either<Throwable, T>>) =
    values.any { it.isRight() }

fun <T> isAllRight(values: List<Either<Throwable, T>>) =
    values.all { it.isRight() }

fun <T> findFirstError(values: List<Either<Throwable, T>>) =
    values.filter { it.isLeft() }.firstNotNullOfOrNull { it.swap().getOrNull() }

fun <T> findFirstRight(values: List<Either<Throwable, T>>) =
    values.filter { it.isRight() }.firstNotNullOfOrNull { it }

fun <T> findFirstDefinedValue(values: List<Either<Throwable, Option<T>>>) =
    values.filter { it.isRight() }.map {
        it.getOrElse { Option.fromNullable(null) }
    }.first { it.isDefined() }

fun <T> findExisting(values: List<Either<Throwable, Option<T>>>) =
    values.filter { it.isRight() }
        .map {
            it.getOrElse { Option.fromNullable(null) }
        }.filter { it.isDefined() }
        .map { it.orNull()!! }
