package io.github.booster.task.util

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse

fun <T> isAllRight(values: List<Either<Throwable, T>>) =
    values.all { it.isRight() }

fun <T> findExisting(values: List<Either<Throwable, Option<T>>>) =
    values.filter { it.isRight() }
        .map {
            it.getOrElse { Option.fromNullable(null) }
        }.filter { it.isDefined() }
        .map { it.orNull()!! }
