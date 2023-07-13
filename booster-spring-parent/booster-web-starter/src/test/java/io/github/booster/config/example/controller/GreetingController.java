package io.github.booster.config.example.controller;

import arrow.core.Either;
import io.github.booster.config.example.dto.GreetingResponse;
import io.github.booster.factories.TaskFactory;
import io.github.booster.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
public class GreetingController {

    private final TaskFactory taskFactory;

    public GreetingController(
            @Autowired TaskFactory taskFactory
    ) {
        this.taskFactory = taskFactory;
    }

    @GetMapping("/hello")
    Mono<Either<Throwable, GreetingResponse>> hello(
            @RequestParam("from") String from,
            @RequestParam("greeting") String greeting
    ) {
        Task<GreetingResponse, GreetingResponse> task =
                taskFactory.getSyncTask(
                        "greeting",
                        greet -> GreetingResponse.builder()
                                .from("server")
                                .greeting(greet.getGreeting())
                                .build()
                );
        return task.execute(
                GreetingResponse.builder()
                        .from(from)
                        .greeting(greeting)
                        .build()
        );
    }
}
