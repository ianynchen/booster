package io.github.booster.web.handler.compression;

import io.github.booster.web.handler.compression.request.CompressionRequestDecorator;
import io.github.booster.web.handler.compression.response.CompressionResponseDecorator;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class ReactiveCompressionFilter implements WebFilter {

    @NotNull
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, @NotNull WebFilterChain chain) {

        List<String> contentEncodings = exchange.getRequest().getHeaders().get(HttpHeaders.CONTENT_ENCODING);
        String contentEncoding = CollectionUtils.isEmpty(contentEncodings) ? "" : contentEncodings.get(0);

        List<String> acceptEncodings = exchange.getRequest().getHeaders().get(HttpHeaders.ACCEPT_ENCODING);
        String acceptEncoding = CollectionUtils.isEmpty(acceptEncodings) ? "" : acceptEncodings.get(0);

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();



        CompressionResponseDecorator decorator = new CompressionResponseDecorator(acceptEncoding, response);

        if (decorator.canCompress()) {
            response.getHeaders().add(HttpHeaders.CONTENT_ENCODING, decorator.getAlgorithm().getAlgorithm());
        }

        return chain.filter(
                exchange.mutate()
                        .request(new CompressionRequestDecorator(contentEncoding, request))
                        .response(new CompressionResponseDecorator(acceptEncoding, response))
                        .build()
        );
    }
}
