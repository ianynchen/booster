package io.github.booster.web.handler;

import arrow.core.Either;
import arrow.core.Option;
import com.google.common.base.Preconditions;
import io.github.booster.web.handler.response.WebResponse;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityResultHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Converts a response from {@link Either} to {@link WebResponse}
 */
public class ResponseHandler extends ResponseEntityResultHandler {

    private static final Logger log = LoggerFactory.getLogger(ResponseHandler.class);

    private static MethodParameter param;

    private final ExceptionConverter exceptionConverter;

    static {
        try {
            //get new params
            param = new MethodParameter(
                    ResponseHandler.class
                            .getDeclaredMethod("methodForParams"),
                    -1
            );
        } catch (NoSuchMethodException e) {
            log.error("no such method", e);
        }
    }

    private static Mono<ResponseEntity<WebResponse>> methodForParams() {
        return null;
    }

    public ResponseHandler(
            List<HttpMessageWriter<?>> writers,
            RequestedContentTypeResolver resolver,
            ExceptionConverter exceptionConverter
    ) {
        super(writers, resolver);
        this.exceptionConverter = exceptionConverter;
    }

    /**
     * Determines if a result can be handled.
     * @param result the result object to check
     * @return returns true if the result is of type {@literal Mono<Either<Throwable, ?>>}, otherwise false.
     */
    @Override
    public boolean supports(HandlerResult result) {
        boolean shouldHandle =  result.getReturnType().resolve() == Mono.class &&
                result.getReturnType().resolveGeneric(0) == Either.class &&
                result.getReturnType().getGeneric(0).resolveGeneric(0) == Throwable.class &&
                result.getReturnType().getGeneric(0).resolveGeneric(1) == Option.class;
        log.debug("booster-web - ResponseHandler should handle: {}", shouldHandle);
        return shouldHandle;
    }

    /**
     * Converts {@literal Mono<Either<Throwable, Option<?>>>} to {@literal Mono<ResponseEntity<WebResponse<?>>>}
     * @param exchange current server exchange
     * @param result the result from the handling
     * @return returns nothing.
     */
    @NotNull
    @Override
    public Mono<Void> handleResult(@NotNull ServerWebExchange exchange, HandlerResult result) {
        Preconditions.checkNotNull(result.getReturnValue(), "response cannot be null");
        Mono<Either<Throwable, Option<?>>> response = (Mono<Either<Throwable, Option<?>>>)result.getReturnValue();

        Either<Throwable, Option<?>> emptyResponse = new Either.Left<>(new IllegalStateException("response is null"));
        response.switchIfEmpty(Mono.just(emptyResponse));

        Mono<ResponseEntity<WebResponse<?>>> webResponse = response.map(resp -> {
            log.debug("booster-web - response: [{}]", resp);
            ResponseEntity<WebResponse<?>> mappedResponse =
                    WebResponse.build(resp, this.exceptionConverter);
            log.debug("booster-web - mapped response: [{}]", mappedResponse);
            return mappedResponse;
        });
        return super.handleResult(exchange, new HandlerResult(result.getHandler(), webResponse, param));
    }
}
