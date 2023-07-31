package io.github.booster.task

import arrow.core.Option
import reactor.core.publisher.Mono

private const val ZERO = 0

fun asyncLengthFunc(request: String): Mono<Option<Int>> =
    Mono.just(
        Option.fromNullable(request.length)
    )

val defaultLengthFuncObj: () -> Option<Int> = {
    Option.fromNullable(0)
}

fun syncLengthFunc(request: String?): Int = request?.length ?: ZERO

private const val EMPTY_STRING = ""

fun asyncStringFunc(request: Int): Mono<String> =
    Mono.just(request.toString())

fun syncLengthFunc(request: Int?): String =
    request?.toString() ?: EMPTY_STRING

fun lengthExceptionThrower(t: Throwable): Int = throw t

val lengthExceptionThrowerObj: (Throwable) -> Option<Int> = { t ->
    throw t
}

@Suppress("UnusedPrivateMember", "UnusedParameter")
fun lengthExceptionHandler(t: Throwable): Int = ZERO

fun stringExceptionThrower(t: Throwable): String = throw t

@Suppress("UnusedPrivateMember", "UnusedParameter")
fun stringExceptionHandler(t: Throwable): String = EMPTY_STRING
