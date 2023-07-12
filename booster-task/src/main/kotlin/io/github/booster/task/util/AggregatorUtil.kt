package io.github.booster.task.util

import arrow.core.Either

fun <T> isAnyRight(values: List<Either<Throwable, T?>>) =
    values.any { it.isRight() }

fun <T> isAllRight(values: List<Either<Throwable, T?>>) =
    values.all { it.isRight() }

fun <T> findFirstError(values: List<Either<Throwable, T?>>) =
    values.filter { it.isLeft() }.firstNotNullOfOrNull { it.swap().getOrNull() }
