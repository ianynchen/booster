package io.github.booster.web.handler;

import arrow.core.Either;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.reactive.HandlerResult;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class ResponseHandlerTest {

    public void testMethod(Mono<Either<Throwable, Integer>> value) {

    }


    @Test
    void shouldGetType() {
        Mono<Either<Throwable, Integer>> value = Mono.just(new Either.Right<>(1));
        Mono<Either<Throwable, Integer>> error = Mono.just(new Either.Left<>(new IllegalStateException("")));

        Method[] methods = ResponseHandlerTest.class.getMethods();
        MethodParameter parameter = null;
        for (Method method: methods) {
            if (method.getName().equals("testMethod")) {
                parameter = new MethodParameter(method, 0);
            }
        }
        assertThat(parameter, notNullValue());

        HandlerResult result = new HandlerResult(this, value, parameter);
        assertThat(result, notNullValue());
        assertThat(result.getReturnType().resolve(), equalTo(Mono.class));
        assertThat(result.getReturnType().resolveGeneric(0), equalTo(Either.class));
        assertThat(result.getReturnType().getGeneric(0).resolveGeneric(0), equalTo(Throwable.class));
    }
}
