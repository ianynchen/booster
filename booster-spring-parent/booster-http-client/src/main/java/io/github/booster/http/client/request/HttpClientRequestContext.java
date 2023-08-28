package io.github.booster.http.client.request;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;

/**
 * HTTP request context
 * @param <Request> request object type
 * @param <Response> response object type
 */
@Builder
@Getter
@ToString
@EqualsAndHashCode
public class HttpClientRequestContext<Request, Response> {

    /**
     * Builder class for {@link HttpClientRequestContext}
     * @param <Request> request type
     * @param <Response> response type
     */
    public static class Builder<Request, Response> {

        /**
         * HTTP method type, cannot be null
         */
        private HttpMethod requestMethod;

        /**
         * Headers to be used
         */
        private HttpHeaders headers = new HttpHeaders();

        /**
         * path to call
         */
        private String path = "";

        /**
         * Path variables used if any
         */
        private Map<String, ?> pathVariables = Map.of();

        /**
         * Query parameters used if any
         */
        private Map<String, List<Object>> queryParameters = Map.of();

        /**
         * Request body. If no request body, set to null
         */
        private Request request;

        /**
         * Request type
         */
        private ParameterizedTypeReference<Request> requestReference;

        /**
         * Response class, this and {@link HttpClientRequestContext#responseReference} cannot both be null
         */
        private Class<Response> responseClass;

        /**
         * Response type, this and {@link HttpClientRequestContext#responseClass} cannot both be null
         */
        private ParameterizedTypeReference<Response> responseReference;

        /**
         * Default constructor that does nothing.
         */
        protected Builder() {
        }

        /**
         * Sets request method
         * @param requestMethod {@link HttpMethod}
         * @return {@link Builder}, throws {@link IllegalArgumentException} if requestMethod is null
         */
        public Builder<Request, Response> requestMethod(HttpMethod requestMethod) {
            Preconditions.checkArgument(requestMethod != null, "request method cannot be null");
            this.requestMethod = requestMethod;
            return this;
        }

        /**
         * Sets HTTP headers
         * @param headers {@link HttpHeaders}
         * @return {@link Builder}
         */
        public Builder<Request, Response> headers(HttpHeaders headers) {
            this.headers = headers == null ? new HttpHeaders() : headers;
            return this;
        }

        /**
         * Sets path
         * @param path path for the HTTP call
         * @return {@link Builder}
         */
        public Builder<Request, Response> path(String path) {
            this.path = path == null ? "" : path;
            return this;
        }

        /**
         * Sets path variables
         * @param pathVariables path variables for the HTTP call
         * @return {@link Builder}
         */
        public Builder<Request, Response> pathVariables(Map<String, ?> pathVariables) {
            this.pathVariables = pathVariables == null ? Map.of() : pathVariables;
            return this;
        }

        /**
         * Sets query parameters
         * @param queryParameters query parameters for the HTTP call
         * @return {@link Builder}
         */
        public Builder<Request, Response> queryParameters(Map<String, List<Object>> queryParameters) {
            this.queryParameters = queryParameters == null ? Map.of() : queryParameters;
            return this;
        }

        /**
         * Sets request body
         * @param request request body for the HTTP call
         * @return {@link Builder}
         */
        public Builder<Request, Response> request(Request request) {
            this.request = request;
            return this;
        }

        /**
         * Sets request reference
         * @param requestReference {@link ParameterizedTypeReference} for the HTTP request body
         * @return {@link Builder}
         */
        public Builder<Request, Response> requestReference(ParameterizedTypeReference<Request> requestReference) {
            this.requestReference = requestReference;
            return this;
        }

        /**
         * Sets response class
         * @param responseClass response {@link Class} for the HTTP call
         * @return {@link Builder}
         */
        public Builder<Request, Response> responseClass(Class<Response> responseClass) {
            this.responseClass = responseClass;
            return this;
        }

        /**
         * Sets response reference
         * @param responseReference {@link ParameterizedTypeReference} for the HTTP response
         * @return {@link Builder}
         */
        public Builder<Request, Response> responseReference(ParameterizedTypeReference<Response> responseReference) {
            this.responseReference = responseReference;
            return this;
        }

        /**
         * Builds the {@link HttpClientRequestContext} object
         * @return {@link HttpClientRequestContext}
         * @throws NullPointerException if request method is not set
         */
        public HttpClientRequestContext<Request, Response> build() {
            return new HttpClientRequestContext<>(
                    this.requestMethod,
                    this.headers,
                    this.path,
                    this.pathVariables,
                    this.queryParameters,
                    this.request,
                    this.requestReference,
                    this.responseClass,
                    this.responseReference
            );
        }
    }

    /**
     * HTTP method type, cannot be null
     */
    @NonNull
    private HttpMethod requestMethod;

    /**
     * Headers to be used
     */
    private HttpHeaders headers;

    /**
     * path to call
     */
    private String path;

    /**
     * Path variables used if any
     */
    private Map<String, ?> pathVariables;

    /**
     * Query parameters used if any
     */
    private Map<String, List<Object>> queryParameters;

    /**
     * Request body. If no request body, set to null
     */
    private Request request;

    /**
     * Request type
     */
    private ParameterizedTypeReference<Request> requestReference;

    /**
     * Response class, this and {@link HttpClientRequestContext#responseReference} cannot both be null
     */
    private Class<Response> responseClass;

    /**
     * Response type, this and {@link HttpClientRequestContext#responseClass} cannot both be null
     */
    private ParameterizedTypeReference<Response> responseReference;

    /**
     * All argument constructor
     * @param requestMethod non-null {@link HttpMethod}
     * @param headers {@link HttpHeaders}
     * @param path path of the request
     * @param pathVariables {@link Map} of path variable keys and values
     * @param queryParameters {@link Map} of query parameter keys and values
     * @param request nullable request body
     * @param requestReference nullable {@link ParameterizedTypeReference} for request
     * @param responseClass nullable response {@link Class}
     * @param responseReference nullable response {@link ParameterizedTypeReference}
     */
    protected HttpClientRequestContext(
            @NotNull HttpMethod requestMethod,
            HttpHeaders headers,
            String path,
            Map<String, ?> pathVariables,
            Map<String, List<Object>> queryParameters,
            Request request,
            ParameterizedTypeReference<Request> requestReference,
            Class<Response> responseClass,
            ParameterizedTypeReference<Response> responseReference
    ) {
        Preconditions.checkArgument(requestMethod != null, "request method cannot be null");
        this.requestMethod = requestMethod;
        this.headers = headers == null ? new HttpHeaders() : headers;
        this.path = path == null ? "" : path;
        this.pathVariables = pathVariables == null ? Map.of() : pathVariables;
        this.queryParameters = queryParameters == null ? Map.of() : queryParameters;
        this.request = request;
        this.requestReference = requestReference;
        this.responseClass = responseClass;
        this.responseReference = responseReference;
    }

    /**
     * Creates {@link Builder} object for {@link HttpClientRequestContext}
     * @return {@link Builder}
     * @param <Request> request type
     * @param <Response> response type
     */
    public static <Request, Response> Builder<Request, Response> builder() {
        return new Builder<>();
    }
}
