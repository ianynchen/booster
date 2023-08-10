package io.github.booster.config.example.controller

import arrow.core.Either
import arrow.core.Option
import arrow.core.Option.Companion.fromNullable
import io.github.booster.config.example.dto.GreetingResponse
import io.github.booster.factories.TaskFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/v1")
class GreetingController(
    @param:Autowired private val taskFactory: TaskFactory
) {
    @GetMapping("/hello")
    fun hello(
        @RequestParam("from") from: String?,
        @RequestParam("greeting") greeting: String?
    ): Mono<Either<Throwable, Option<GreetingResponse>>> {
        val task = taskFactory.getSyncTask(
            "greeting"
        ) { greet: GreetingResponse ->
            fromNullable(
                GreetingResponse("server", greet.greeting)
            )
        }
        return task.execute(
            fromNullable(
                GreetingResponse(from, greeting)
            )
        )
    }
}
