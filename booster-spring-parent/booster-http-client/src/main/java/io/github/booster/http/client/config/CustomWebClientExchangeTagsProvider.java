package io.github.booster.http.client.config;

import io.micrometer.core.instrument.Tag;
import org.springframework.boot.actuate.metrics.web.reactive.client.DefaultWebClientExchangeTagsProvider;
import org.springframework.boot.actuate.metrics.web.reactive.client.WebClientExchangeTags;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;

import static java.util.Arrays.asList;

/**
 * Adds custom tags for {@link org.springframework.web.reactive.function.client.WebClient}
 */
public class CustomWebClientExchangeTagsProvider extends DefaultWebClientExchangeTagsProvider {
    /**
     * URI tags
     */
    public static final String URI_ATTRIBUTE = "custom.webclient.uri";
    /**
     * client name tags
     */
    public static final String CLIENT_NAME_ATTRIBUTE = "custom.client.name";

    /**
     * Default constructor
     */
    public CustomWebClientExchangeTagsProvider() {
    }

    /**
     * Gets {@link Tag}s for a request/response pair
     * @param request the client request
     * @param response the server response (may be {@code null})
     * @param throwable the exception (may be {@code null})
     * @return a list available tags
     */
    @Override
    public Iterable<Tag> tags(ClientRequest request, ClientResponse response, Throwable throwable) {
        Tag method = WebClientExchangeTags.method(request);
        Tag uri = getUriTag(request);
        Tag clientName = getClientNameTag(request);
        return asList(method, uri, clientName, WebClientExchangeTags.status(response, throwable), WebClientExchangeTags.outcome(response));
    }

    private Tag getClientNameTag(ClientRequest request) {
        return request.attribute(CLIENT_NAME_ATTRIBUTE)
                .map(name -> Tag.of("client_name", (String) name))
                .orElse(WebClientExchangeTags.clientName(request));
    }

    private Tag getUriTag(ClientRequest request) {
        return request.attribute(URI_ATTRIBUTE)
                .map(uri -> Tag.of("uri", (String) uri))
                .orElse(WebClientExchangeTags.uri(request));
    }
}
