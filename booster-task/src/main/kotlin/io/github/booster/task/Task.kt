package io.github.booster.task

import arrow.core.Either
import arrow.core.Option
import reactor.core.publisher.Mono

typealias DataWithError<T> = Either<Throwable, Option<T>>

typealias RequestExceptionHandler<T> = (Throwable) -> Option<T>
typealias EmptyRequestHandler<T> = () -> Option<T>

/**
 * Task execution.
 * @param <Request> request object type.
 * @param <Response> response object type.
 */
interface Task<Request, Response> {

    fun execute(request: Request) =
        this.execute(Option.fromNullable(request))

    /**
     * Execute task with a request object.
     * @param request Request object
     * @return returns a response with exception wrapped inside an [Either]
     */
    fun execute(request: Option<Request>) =
        execute(Either.Right(request))

    /**
     * Execute task with a request object.
     * @param request Request object as [Either]
     * @return returns a response with exception wrapped inside an [Either]
     */
    fun execute(request: DataWithError<Request>) =
        execute(Mono.just(request))

    /**
     * Execute task with a request object.
     * @param request Request object as [Mono]
     * @return returns a response with exception wrapped inside an [Either]
     */
    fun execute(request: Mono<DataWithError<Request>>): Mono<DataWithError<Response>>

    /**
     * Name of the task.
     * @return name of the task
     */
    val name: String
}
