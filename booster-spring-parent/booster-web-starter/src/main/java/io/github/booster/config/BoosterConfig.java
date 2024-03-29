package io.github.booster.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.booster.commons.circuit.breaker.CircuitBreakerConfig;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.retry.RetryConfig;
import io.github.booster.config.thread.ThreadPoolConfig;
import io.github.booster.factories.HttpClientFactory;
import io.github.booster.factories.TaskFactory;
import io.github.booster.http.client.config.CustomWebClientExchangeTagsProvider;
import io.github.booster.http.client.config.HttpClientConnectionConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.actuate.metrics.web.reactive.client.WebClientExchangeTagsProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Auto configuration for Booster Web, HTTP client and tasks.
 */
@Configuration
public class BoosterConfig implements ApplicationContextAware {

    /**
     * Service tag
     */
    public static final String NAME = "service";

    /**
     * Environment tag
     */
    public static final String PROFILE = "environment";

    private static final Logger log = LoggerFactory.getLogger(BoosterConfig.class);

    private ApplicationContext applicationContext;

    /**
     * Default constructor
     */
    public BoosterConfig() {
    }

    /**
     * Creates metrics recorder
     *
     * @param meterRegistry internal {@link MeterRegistry} to be used to record metrics
     * @param recordTrace   whether to include trace ID in metrics as a tag. If set to true, will add traceId and value of the trace in tags.
     * @return {@link MetricsRegistry}
     */
    @Bean
    public MetricsRegistry metricsRegistry(
            @Autowired(required = false) MeterRegistry meterRegistry,
            @Value("${booster.metrics.recordTrace:false}") boolean recordTrace
    ) {
        log.debug("booster-starter - record with trace: [{}]", recordTrace);
        return new MetricsRegistry(meterRegistry, recordTrace);
    }

    /**
     * Creates a {@link ThreadPoolConfig} bean
     * @param registry {@link MetricsRegistry} to record metrics
     * @return {@link ThreadPoolConfig} bean
     */
    @Bean
    @ConfigurationProperties(prefix = "booster.tasks.threads")
    public ThreadPoolConfig threadPoolConfig(
            @Autowired MetricsRegistry registry
    ) {
        return new ThreadPoolConfig(
                this.applicationContext,
                registry
        );
    }

    /**
     * Creates a {@link CircuitBreakerConfig} bean
     * @return {@link CircuitBreakerConfig} bean
     */
    @Bean
    @ConfigurationProperties(prefix = "booster.tasks.circuit-breakers")
    public CircuitBreakerConfig circuitBreakerConfig() {
        return new CircuitBreakerConfig();
    }

    /**
     * Creates a {@link HttpClientConnectionConfig} bean
     * @return {@link HttpClientConnectionConfig} bean
     */
    @Bean
    @ConfigurationProperties(prefix = "booster.http.clients.connections")
    public HttpClientConnectionConfig httpClientConnectionConfig() {
        return new HttpClientConnectionConfig(this.applicationContext);
    }

    /**
     * Creates a {@link WebClientExchangeTagsProvider} bean
     * @return {@link WebClientExchangeTagsProvider} bean
     */
    @Bean
    public WebClientExchangeTagsProvider clientRequestObservationConvention() {
        return new CustomWebClientExchangeTagsProvider();
    }

    /**
     * Creates a {@link RetryConfig} bean
     * @return {@link RetryConfig} bean
     */
    @Bean
    @ConfigurationProperties(prefix = "booster.tasks.retries")
    public RetryConfig retryConfig() {
        return new RetryConfig();
    }

    /**
     * Creates {@link HttpClientFactory} bean
     * @param config {@link HttpClientConnectionConfig}
     * @param webClientBuilder {@link WebClient.Builder} for trace injection
     * @param mapper {@link ObjectMapper} for serialization and deserialization of requests/responses
     * @return {@link HttpClientFactory} bean
     */
    @Bean
    public HttpClientFactory httpClientFactory(
            @Autowired HttpClientConnectionConfig config,
            @Autowired WebClient.Builder webClientBuilder,
            @Autowired(required = false) ObjectMapper mapper
    ) {
        return new HttpClientFactory(config, webClientBuilder, mapper);
    }

    /**
     * Creates {@link TaskFactory} bean
     * @param threadPoolConfig {@link ThreadPoolConfig} for {@link io.github.booster.task.Task}
     * @param retryConfig {@link RetryConfig} for {@link io.github.booster.task.Task}
     * @param circuitBreakerConfig {@link CircuitBreakerConfig} for {@link io.github.booster.task.Task}
     * @param httpClientFactory {@link HttpClientFactory} to create HTTP {@link io.github.booster.task.Task}
     * @param registry {@link MetricsRegistry} to record metrics
     * @return {@link TaskFactory} bean
     */
    @Bean
    public TaskFactory TaskFactory(
            @Autowired ThreadPoolConfig threadPoolConfig,
            @Autowired RetryConfig retryConfig,
            @Autowired CircuitBreakerConfig circuitBreakerConfig,
            @Autowired HttpClientFactory httpClientFactory,
            @Autowired MetricsRegistry registry
    ) {
        return new TaskFactory(
                threadPoolConfig,
                retryConfig,
                circuitBreakerConfig,
                httpClientFactory,
                registry
        );
    }

    /**
     * Creates {@link MeterRegistryCustomizer} bean to inject common tags on metrics
     * @param serviceName name of service
     * @param activeProfile name of environment
     * @return {@link MeterRegistryCustomizer} bean
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            @Value("${spring.application.name:default}") String serviceName,
            @Value("${spring.profiles.active:default}") String activeProfile
    ) {
        return registry -> registry.config().commonTags(
                NAME, serviceName,
                PROFILE, activeProfile
        );
    }

    /**
     * Sets application context
     * @param applicationContext the ApplicationContext object to be used by this object
     * @throws BeansException throws {@link BeansException}
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
