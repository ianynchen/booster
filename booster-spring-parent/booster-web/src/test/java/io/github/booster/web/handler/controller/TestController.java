package io.github.booster.web.handler.controller;

import arrow.core.Either;
import arrow.core.Option;
import io.github.booster.commons.util.EitherUtil;
import io.github.booster.web.handler.dto.Greeting;
import io.github.booster.web.handler.response.WebException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

@RestController
@RequestMapping("/api/v1")
public class TestController {

    @GetMapping(
            value = "/hello",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<Either<Throwable, Option<Greeting>>> hello(
        @RequestParam("from") String from,
        @RequestParam("greeting") String greeting
    ) {
        if (Objects.equals(from, "death")) {
            return Mono.just(EitherUtil.convertThrowable(new IllegalStateException("illegal state")));
        } else if (Objects.equals(greeting, "hola")) {
            WebException exception = new WebException(
                    HttpStatus.BAD_REQUEST,
                    HttpStatus.BAD_REQUEST.name(),
                    "unknown language",
                    "unknown language"
            );
            return Mono.just(EitherUtil.convertThrowable(exception));
        } else {
            return Mono.just(EitherUtil.convertData(Option.fromNullable(new Greeting("server", from, greeting))));
        }
    }
}
