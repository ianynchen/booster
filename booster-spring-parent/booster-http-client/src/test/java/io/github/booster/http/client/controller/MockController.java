package io.github.booster.http.client.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/v1/hello")
public class MockController {

    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    public static class Greeting {
        private String greeting;
        private String from;
        private String to;
    }

    @GetMapping("/query")
    public Mono<ResponseEntity<Greeting>> query(
            @RequestHeader(HttpHeaders.ACCEPT_LANGUAGE) String language,
            @RequestParam("name") String name
    ) {
        if (language.equalsIgnoreCase("en")) {
            return Mono.just(ResponseEntity.ok(new Greeting("Hello", "Server", name)));
        } else {
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }

    @PostMapping("/")
    public Mono<ResponseEntity<Greeting>> post(
            @RequestHeader(HttpHeaders.ACCEPT_LANGUAGE) String language,
            @RequestBody Greeting greeting
    ) {
        if (language.equalsIgnoreCase("en")) {
            return Mono.just(ResponseEntity.ok(new Greeting("Hello", "Server", greeting.getFrom())));
        } else {
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }

    @GetMapping("/{name}")
    public Mono<ResponseEntity<Greeting>> hello(
            @RequestHeader(HttpHeaders.ACCEPT_LANGUAGE) String language,
            @PathVariable("name") String name
    ) {
        if (language.equalsIgnoreCase("en")) {
            return Mono.just(ResponseEntity.ok(new Greeting("Hello", "Server", name)));
        } else {
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }
}
