package io.github.booster.http.client.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import io.github.booster.http.client.HttpClient;
import io.github.booster.http.client.config.CustomWebClientExchangeTagsProvider;
import io.github.booster.http.client.config.HttpClientConnectionConfig;
import io.github.booster.http.client.request.HttpClientRequestContext;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Internal HttpClient class that wraps functionality over {@link WebClient}
 */
public class HttpClientImpl<Request, Response> implements HttpClient<Request, Response> {

    private final static Logger log = LoggerFactory.getLogger(HttpClientImpl.class);

    private final String name;

    private final WebClient webClient;

    private final ObjectMapper mapper;

    /**
     * Construct HttpClient from {@link HttpClientConnectionConfig}
     * @param name name of the setting to use
     * @param httpClientConnectionConfig {@link HttpClientConnectionConfig} object to create client from.
     */
    public HttpClientImpl(
            WebClient.Builder webClientBuilder,
            String name,
            HttpClientConnectionConfig httpClientConnectionConfig
    ) {
        this(webClientBuilder, name, httpClientConnectionConfig, null);
    }

    /**
     * Construct HttpClient from {@link HttpClientConnectionConfig}
     * @param name name of the setting to use
     * @param httpClientConnectionConfig {@link HttpClientConnectionConfig} object to create client from.
     * @param mapper {@link ObjectMapper} used to log request and response objects.
     */
    public HttpClientImpl(
            WebClient.Builder webClientBuilder,
            String name,
            HttpClientConnectionConfig httpClientConnectionConfig,
            ObjectMapper mapper
    ) {
        this(name, httpClientConnectionConfig.create(webClientBuilder, name), mapper);
    }

    /**
     * Construct HttpClient using previously created {@link WebClient}
     * @param name name of the setting to use
     * @param client {@link WebClient} to use
     */
    public HttpClientImpl(String name, WebClient client) {
        this(name, client, null);
    }

    /**
     * Construct HttpClient using previously created {@link WebClient}
     * @param name name of the setting to use
     * @param client {@link WebClient} to use
     * @param mapper {@link ObjectMapper} used to log request and response objects.
     */
    public HttpClientImpl(String name, WebClient client, ObjectMapper mapper) {
        this.name = name;
        this.webClient = client;
        this.mapper = mapper;
    }

    /**
     * Invoke call to server
     * @param context request to use
     * @return {@link ResponseEntity} of designated response type.
     */
    @Override
    public Mono<ResponseEntity<Response>> invoke(HttpClientRequestContext<Request, Response> context) {
        Preconditions.checkArgument(context != null, "request context cannot be null");

        Request request = context.getRequest();
        ParameterizedTypeReference<Response> responseReference = context.getResponseReference();
        Class<Response> responseClass = context.getResponseClass();

        Preconditions.checkArgument(responseReference != null || responseClass != null, "response class and ParameterizedTypeReference cannot be both null");
        Mono<ResponseEntity<Response>> responseEntity;
        if (context.getRequestReference() == null) {
            if (context.getResponseClass() != null) {
                responseEntity = this.invoke(
                        context.getRequestMethod(),
                        context.getHeaders(),
                        context.getPath(),
                        context.getPathVariables(),
                        context.getQueryParameters(),
                        context.getRequest(),
                        context.getResponseClass()
                );
            } else { //if (context.getResponseReference() != null) {
                responseEntity = this.invoke(
                        context.getRequestMethod(),
                        context.getHeaders(),
                        context.getPath(),
                        context.getPathVariables(),
                        context.getQueryParameters(),
                        context.getRequest(),
                        context.getResponseReference()
                );
            }
        } else { // request reference is not null
            if (context.getResponseClass() != null) {
                responseEntity = this.invoke(
                        context.getRequestMethod(),
                        context.getHeaders(),
                        context.getPath(),
                        context.getPathVariables(),
                        context.getQueryParameters(),
                        context.getRequest(),
                        context.getRequestReference(),
                        context.getResponseClass()
                );
            } else { //if (context.getResponseReference() != null) {
                responseEntity = this.invoke(
                        context.getRequestMethod(),
                        context.getHeaders(),
                        context.getPath(),
                        context.getPathVariables(),
                        context.getQueryParameters(),
                        context.getRequest(),
                        context.getRequestReference(),
                        context.getResponseReference()
                );
            }
        }
        return responseEntity.map(response -> {
            log.debug(
                    "booster-http-client - status code: [{}], response: [{}]",
                    response.getStatusCode(),
                    getLogObject(response.getBody())
            );
            return response;
        });
    }

    private WebClient.RequestBodySpec buildRequestBodySpec(
            HttpMethod method,
            HttpHeaders headers,
            String path,
            Map<String, ?> pathVariables,
            Map<String, List<Object>> queryParameters
    ) {
        log.debug("booster-http-client - request method: [{}]", method);
        log.debug("booster-http-client - headers: [{}]", headers);
        log.debug("booster-http-client - path: [{}]", path);
        log.debug("booster-http-client - path variables: [{}]", pathVariables);
        log.debug("booster-http-client - query parameters: [{}]", queryParameters);

        val requestBodySpec = this.webClient
                .method(method).uri(uriBuilder -> {
                    uriBuilder.path(path);

                    if (queryParameters != null && !queryParameters.isEmpty()) {
                        queryParameters.entrySet().forEach(entry -> {
                            uriBuilder.queryParam(entry.getKey(), "{" + entry.getKey() + "}");
                            uriBuilder.replaceQueryParam(entry.getKey(), entry.getValue());
                        });
                    }

                    if (pathVariables != null && !pathVariables.isEmpty()) {
                        return uriBuilder.build(pathVariables);
                    } else {
                        return uriBuilder.build();
                    }
                }).attribute(CustomWebClientExchangeTagsProvider.CLIENT_NAME_ATTRIBUTE, this.name)
                .attribute(CustomWebClientExchangeTagsProvider.URI_ATTRIBUTE, path);
        if (headers != null && !headers.isEmpty()) {
            headers.forEach((key, values) -> requestBodySpec.header(key, values.toArray(new String[0])));
        }
        return requestBodySpec;
    }

    private Mono<ResponseEntity<Response>> invoke(
            HttpMethod method,
            HttpHeaders headers,
            String path,
            Map<String, ?> pathVariables,
            Map<String, List<Object>> queryParameters,
            Request request,
            Class<Response> responseClass
    ) {
        val requestBodySpec = buildRequestBodySpec(method, headers, path, pathVariables, queryParameters);
        log.debug("booster-http-client - request body: [{}]", getLogObject(request));

        Mono<ResponseEntity<Response>> response;
        if (request != null) {
            response = requestBodySpec.body(Mono.just(request), request.getClass()).retrieve().toEntity(responseClass);
        } else {
            response = requestBodySpec.retrieve().toEntity(responseClass);
        }
        return response;
    }

    private Mono<ResponseEntity<Response>> invoke(
            HttpMethod method,
            HttpHeaders headers,
            String path,
            Map<String, ?> pathVariables,
            Map<String, List<Object>> queryParameters,
            Request request,
            ParameterizedTypeReference<Response> responseType
    ) {
        val requestBodySpec = buildRequestBodySpec(method, headers, path, pathVariables, queryParameters);
        log.debug("booster-http-client - request body: [{}]", getLogObject(request));

        Mono<ResponseEntity<Response>> response;
        if (request != null) {
            response = requestBodySpec.body(Mono.just(request), request.getClass()).retrieve().toEntity(responseType);
        } else {
            response = requestBodySpec.retrieve().toEntity(responseType);
        }
        return response;
    }

    private Mono<ResponseEntity<Response>> invoke(
            HttpMethod method,
            HttpHeaders headers,
            String path,
            Map<String, ?> pathVariables,
            Map<String, List<Object>> queryParameters,
            Request request,
            ParameterizedTypeReference<Request> requestType,
            Class<Response> responseClass
    ) {
        val requestBodySpec = buildRequestBodySpec(method, headers, path, pathVariables, queryParameters);
        log.debug("booster-http-client - request body: [{}]", getLogObject(request));

        Mono<ResponseEntity<Response>> response;
        if (request != null) {
            response = requestBodySpec.body(Mono.just(request), requestType).retrieve().toEntity(responseClass);
        } else {
            response = requestBodySpec.retrieve().toEntity(responseClass);
        }
        return response;
    }

    private Mono<ResponseEntity<Response>> invoke(
            HttpMethod method,
            HttpHeaders headers,
            String path,
            Map<String, ?> pathVariables,
            Map<String, List<Object>> queryParameters,
            Request request,
            ParameterizedTypeReference<Request> requestType,
            ParameterizedTypeReference<Response> responseType
    ) {
        val requestBodySpec = buildRequestBodySpec(method, headers, path, pathVariables, queryParameters);
        log.debug("booster-http-client - request body: [{}]", getLogObject(request));

        Mono<ResponseEntity<Response>> response;
        if (request != null) {
            response = requestBodySpec.body(Mono.just(request), requestType).retrieve().toEntity(responseType);
        } else {
            response = requestBodySpec.retrieve().toEntity(responseType);
        }
        return response;
    }

    private Object getLogObject(Object o) {
        if (o == null) {
            return null;
        } else {
            if (this.mapper != null) {

                try {
                    return this.mapper.writeValueAsString(o);
                } catch (JsonProcessingException e) {
                    log.error("booster-http-client - error serializing object: [{}]", o, e);
                    return o;
                }
            } else {
                return o;
            }
        }
    }
}
