package io.github.booster.http.client;

import io.github.booster.http.client.request.HttpClientRequestContext;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

/**
 * HTTP client interface
 * @param <Request> Request type
 * @param <Response> Response type
 */
public interface HttpClient<Request, Response> {

    /**
     * Invoke call to server
     * @param context request to use
     * @return {@link ResponseEntity} of designated response type.
     */
    Mono<ResponseEntity<Response>> invoke(HttpClientRequestContext<Request, Response> context);
}
