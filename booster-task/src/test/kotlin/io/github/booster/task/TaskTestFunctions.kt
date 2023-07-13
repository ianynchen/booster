package io.github.booster.task

import reactor.core.publisher.Mono

private const val ZERO = 0

fun asyncLengthFunc(request: String?): Mono<Int> =
    Mono.just(
        request?.length ?: ZERO
    )

fun syncLengthFunc(request: String?): Int = request?.length ?: ZERO

private const val EMPTY_STRING = ""

fun asyncStringFunc(request: Int?): Mono<String> =
    Mono.just(request?.toString() ?: EMPTY_STRING)

fun syncLengthFunc(request: Int?): String =
    request?.toString() ?: EMPTY_STRING

fun lengthExceptionThrower(t: Throwable): Int = throw t

@Suppress("UnusedPrivateMember")
fun lengthExceptionHandler(t: Throwable): Int = ZERO

fun stringExceptionThrower(t: Throwable): String = throw t

@Suppress("UnusedPrivateMember")
fun stringExceptionHandler(t: Throwable): String = EMPTY_STRING
