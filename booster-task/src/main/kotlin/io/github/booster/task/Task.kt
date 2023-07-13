package io.github.booster.task

import arrow.core.Either
import reactor.core.publisher.Mono

/**
 * Task execution.
 * @param <Request> request object type.
 * @param <Response> response object type.
 */
interface Task<Request, Response> {

    /**
     * Execute task with a request object.
     * @param request Request object
     * @return returns a response with exception wrapped inside an [Either]
     */
    fun execute(request: Request?) =
        execute(Either.Right(request))

    /**
     * Execute task with a request object.
     * @param request Request object as [Either]
     * @return returns a response with exception wrapped inside an [Either]
     */
    fun execute(request: Either<Throwable, Request?>) =
        execute(Mono.just(request))

    /**
     * Execute task with a request object.
     * @param request Request object as [Mono]
     * @return returns a response with exception wrapped inside an [Either]
     */
    fun execute(request: Mono<Either<Throwable, Request?>>): Mono<Either<Throwable, Response>>

    /**
     * Name of the task.
     * @return name of the task
     */
    val name: String
}
