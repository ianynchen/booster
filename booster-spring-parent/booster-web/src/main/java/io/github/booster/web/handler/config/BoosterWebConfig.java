package io.github.booster.web.handler.config;

import arrow.core.Either;
import arrow.core.Option;
import com.fasterxml.classmate.TypeResolver;
import io.github.booster.web.handler.ExceptionConverter;
import io.github.booster.web.handler.ExceptionHandler;
import io.github.booster.web.handler.ResponseHandler;
import io.github.booster.web.handler.response.WebResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import reactor.core.publisher.Mono;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.AlternateTypeRules;
import springfox.documentation.schema.WildcardType;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.List;

@Configuration
public class BoosterWebConfig {

    @Bean
    public ExceptionConverter exceptionConverter(
            @Autowired(required = false)
            List<ExceptionHandler<?>> handlers
    ) {
        return new ExceptionConverter(handlers);
    }

    @Bean
    public ResponseHandler responseHandler(
            @Autowired ServerCodecConfigurer serverCodecConfigurer,
            @Autowired RequestedContentTypeResolver requestedContentTypeResolver,
            @Autowired ExceptionConverter exceptionConverter
    ) {
        return new ResponseHandler(
                serverCodecConfigurer.getWriters(),
                requestedContentTypeResolver,
                exceptionConverter
        );
    }

    @Bean
    public Docket api(TypeResolver typeResolver) {

        // annotate only RestControllers, and convert
        // Mono<Either<Throwable, T>> to WebResponse<T>
        return new Docket(DocumentationType.OAS_30)
                .select()
                .apis(RequestHandlerSelectors.withClassAnnotation(RestController.class))
                .paths(PathSelectors.any())
                .build()
                .alternateTypeRules(
                        AlternateTypeRules.newRule(
                                typeResolver.resolve(
                                        Mono.class,
                                        typeResolver.resolve(
                                                Either.class,
                                                Throwable.class,
                                                typeResolver.resolve(
                                                        Option.class,
                                                        WildcardType.class
                                                )
                                        )
                                ),
                                typeResolver.resolve(
                                        WebResponse.class,
                                        WildcardType.class
                                ),
                                Ordered.HIGHEST_PRECEDENCE
                        ),
                        AlternateTypeRules.newRule(
                                typeResolver.resolve(
                                        Either.class,
                                        Throwable.class,
                                        typeResolver.resolve(
                                                Option.class,
                                                WildcardType.class
                                        )
                                ),
                                typeResolver.resolve(
                                        WebResponse.class,
                                        WildcardType.class
                                ),
                                Ordered.HIGHEST_PRECEDENCE
                        )
                );
    }
}
