package io.github.booster.http.client.config;

import com.google.common.base.Preconditions;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.DefaultSslContextSpec;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.of;

/**
 * HTTP client configuration object.
 */
public class HttpClientConnectionConfig {

    private static final Logger log = LoggerFactory.getLogger(HttpClientConnectionConfig.class);

    private Map<String, HttpClientConnectionSetting> settings;

    private final Map<String, WebClient> clients = new ConcurrentHashMap<>();

    private final ApplicationContext applicationContext;

    public HttpClientConnectionConfig(
            ApplicationContext applicationContext
    ) {
        this.applicationContext = applicationContext;
        this.settings = Map.of();
    }

    public void setSettings(Map<String, HttpClientConnectionSetting> settings) {
        this.settings = settings == null ? Map.of() : settings;
    }

    /**
     * Creates a reactive {@link WebClient}. Each WebClient object is cached in the
     * configuration object and if one with the same name is already created, the previously
     * created instance will be returned.
     *
     * @param name name of {@link WebClient}
     * @return {@link WebClient} created
     */
    public WebClient create(@NotNull WebClient.Builder webClientBuilder, String name) {

        return clients.computeIfAbsent(name, key -> {
            HttpClientConnectionSetting connectionSetting = null;
            if (StringUtils.isNotEmpty(name) && settings.containsKey(name)) {
                connectionSetting = settings.get(name);
            }
            Preconditions.checkArgument(connectionSetting != null, "connection setting cannot be null");

            log.debug("booster-http-client - building client for [{}] with setting: [{}]", name, connectionSetting);
            final WebClient.Builder builder = webClientBuilder
                    .baseUrl(connectionSetting.getBaseUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .clientConnector(new ReactorClientHttpConnector(this.createHttpClient(name, connectionSetting)));

            of(connectionSetting).ifPresent(b -> addMetrics(builder));
            of(connectionSetting)
                    .map(HttpClientConnectionSetting::getMaxInMemorySizeMB)
                    .filter(maxInMemorySize -> maxInMemorySize > 0)
                    .ifPresent(maxInMemorySize -> builder.codecs(cfr -> cfr.defaultCodecs().maxInMemorySize(maxInMemorySize * 1024 * 1024)));
            return builder.build();
        });
    }

    private void addMetrics(WebClient.Builder builder) {
        if (this.applicationContext != null) {
            final MetricsWebClientCustomizer customizer =
                    this.applicationContext.getBean(MetricsWebClientCustomizer.class);
            customizer.customize(builder);
            log.debug("booster-http-client - customized with client metrics.");
        }
    }

    private HttpClient createHttpClient(String name, HttpClientConnectionSetting setting) {

        Preconditions.checkArgument(StringUtils.isNotEmpty(name), "http client connection setting name cannot be null");
        HttpClient httpClient = HttpClient.create(createConnectionProvider(name, setting.getPool()))
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .doOnConnected(con -> {
                    if (setting.getReadTimeoutMillis() > 0) {
                        con.addHandlerLast(new ReadTimeoutHandler(setting.getReadTimeoutMillis(), TimeUnit.MILLISECONDS));
                    }
                    if (setting.getWriteTimeoutMillis() > 0) {
                        con.addHandlerLast(new WriteTimeoutHandler(setting.getWriteTimeoutMillis(), TimeUnit.MILLISECONDS));
                    }
                }).compress(true);
        return of(httpClient)
                .map(hc -> hc.followRedirect(setting.isFollowRedirects()))
                .map(hc -> hc.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, setting.getConnectionTimeoutMillis()))
                .map(hc -> setting.isUseSSL() ? hc.secure(spec -> spec.sslContext(DefaultSslContextSpec.forClient()).handshakeTimeout(Duration.ofMillis(setting.getSslHandshakeTimeoutMillis()))) : hc)
                .map(hc -> setting.getResponseTimeoutInMillis() != null && setting.getResponseTimeoutInMillis() > 0L ? hc.responseTimeout(Duration.ofMillis(setting.getResponseTimeoutInMillis())) : hc)
                .orElse(null);
    }

    private ConnectionProvider createConnectionProvider(String name, HttpClientConnectionSetting.ConnectionPoolSetting setting) {

        ConnectionProvider.Builder builder = ConnectionProvider.builder(name);
        if (setting.getMaxConnections() != null) {
            builder.maxConnections(setting.getMaxConnections());
        }
        if (setting.getMaxIdleTimeMillis() != null) {
            builder.maxIdleTime(Duration.ofMillis(setting.getMaxIdleTimeMillis()));
        }
        if (setting.getMaxLifeTimeMillis() != null) {
            builder.maxLifeTime(Duration.ofMillis(setting.getMaxLifeTimeMillis()));
        }
        return builder.metrics(true).build();
    }
}
