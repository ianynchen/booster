package io.github.booster.http.client.request;

import lombok.Builder;
import lombok.NonNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.Map;

/**
 * HTTP reqeust context
 * @param <Request> request object type
 * @param <Response> response object type
 */
@Builder
public class HttpClientRequestContext<Request, Response> {

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
     * {@link UserContext} to be used. This adds to headers in the request.
     */
    private UserContext userContext;

    public HttpMethod getRequestMethod() {
        return requestMethod;
    }

    public HttpHeaders getHeaders() {
        if (this.headers == null) {
            this.headers = new HttpHeaders();
        }
        if (this.userContext != null) {
            Map<String, String> headerValues = this.userContext.createHeaders();
            for (String key: headerValues.keySet()) {
                this.headers.put(key, List.of(headerValues.get(key)));
            }
        }
        return this.headers;
    }

    public String getPath() {
        return this.path == null ? "" : this.path;
    }

    public Map<String, ?> getPathVariables() {
        return this.pathVariables == null ? Map.of() : this.pathVariables;
    }

    public Map<String, List<Object>> getQueryParameters() {
        return this.queryParameters == null ? Map.of() : this.queryParameters;
    }

    public Request getRequest() {
        return request;
    }

    public ParameterizedTypeReference<Request> getRequestReference() {
        return requestReference;
    }

    public Class<Response> getResponseClass() {
        return responseClass;
    }

    public ParameterizedTypeReference<Response> getResponseReference() {
        return responseReference;
    }
}
