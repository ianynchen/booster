package io.github.booster.http.client.config;

import com.google.common.base.Preconditions;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.resources.ConnectionProvider;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * <p>Http client connection setting. Response compression,
 * client metrics recording and connection pool metrics are
 * all enabled.</p>
 *
 * <p>Configurations currently supported:</p>
 *
 * <table>
 *     <caption>HTTP Connection Configuration Elements</caption>
 *     <tr>
 *         <th>Category</th>
 *         <th>Name</th>
 *         <th>Meaning</th>
 *         <th>Default Value</th>
 *     </tr>
 *     <tr>
 *         <td>Connection</td>
 *         <td>baseUrl</td>
 *         <td>Base URL for client</td>
 *         <td>None</td>
 *     </tr>
 *     <tr>
 *         <td>Connection</td>
 *         <td>connectionTimeoutMillis</td>
 *         <td>Connection timeout in milliseconds</td>
 *         <td>200</td>
 *     </tr>
 *     <tr>
 *         <td>Connection</td>
 *         <td>readTimeoutMillis</td>
 *         <td>Connection timeout in milliseconds</td>
 *         <td>200</td>
 *     </tr>
 *     <tr>
 *         <td>Connection</td>
 *         <td>writeTimeoutMillis</td>
 *         <td>Connection timeout in milliseconds</td>
 *         <td>200</td>
 *     </tr>
 *     <tr>
 *         <td>Connection</td>
 *         <td>responseTimeoutInMillis</td>
 *         <td>Connection timeout in milliseconds</td>
 *         <td>200</td>
 *     </tr>
 *     <tr>
 *         <td>Connection</td>
 *         <td>useSSL</td>
 *         <td>Is connection SSL based</td>
 *         <td>False</td>
 *     </tr>
 *     <tr>
 *         <td>Connection</td>
 *         <td>sslHandshakeTimeoutMillis</td>
 *         <td>SSL handshake timeout in milliseconds, applies if and only if SSL is used</td>
 *         <td>10,000</td>
 *     </tr>
 *     <tr>
 *         <td>Connection</td>
 *         <td>maxInMemorySizeMB</td>
 *         <td>Maximum memory size in MB</td>
 *         <td>256</td>
 *     </tr>
 *     <tr>
 *         <td>Connection Pool</td>
 *         <td>maxConnections</td>
 *         <td>Maximum connections pooled</td>
 *         <td>2 * max(available_processors, 8)</td>
 *     </tr>
 *     <tr>
 *         <td>Connection Pool</td>
 *         <td>maxIdleTimeMillis</td>
 *         <td>Maximum connection idle timeout in milliseconds</td>
 *         <td>No limit</td>
 *     </tr>
 *     <tr>
 *         <td>Connection Pool</td>
 *         <td>maxLifeTimeMillis</td>
 *         <td>Maximum connection life time in milliseconds</td>
 *         <td>No limit</td>
 *     </tr>
 * </table>
 */
@ToString
public class HttpClientConnectionSetting {

    /**
     * Default connection timeout milliseconds
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT_MS = 200;

    /**
     * Default read timeout milliseconds
     */
    public static final int DEFAULT_READ_TIMEOUT_MS = 200;

    /**
     * Default write timeout milliseconds
     */
    public static final int DEFAULT_WRITE_TIMEOUT_MS = 200;

    /**
     * Default maximum in memory size megabytes
     */
    public static final int DEFAULT_MAX_IN_MEMORY_SIZE_MB = 256;

    /**
     * Default SSL handshake timeout in milliseconds
     */
    public static final int DEFAULT_HANDSHAKE_TIMEOUT_MS = 10 * 1000;

    /**
     * HTTP connection pool settings.
     */
    @ToString
    public static class ConnectionPoolSetting {

        /**
         * Default maximum connections
         */
        public static final Integer DEFAULT_MAX_CONNECTIONS = ConnectionProvider.DEFAULT_POOL_MAX_CONNECTIONS;

        /**
         * Default maximum idle time milliseconds
         */
        public static final Long DEFAULT_MAX_IDLE_TIME_MILLIS = ConnectionProvider.DEFAULT_POOL_MAX_IDLE_TIME;

        /**
         * Default maximum life time milliseconds
         */
        public static final Long DEFAULT_MAX_LIFE_TIME_MILLIS = ConnectionProvider.DEFAULT_POOL_MAX_LIFE_TIME;

        /**
         * Maximum number of connections for connection pool
         */
        private Integer maxConnections;

        /**
         * Maximum number of idle time in milliseconds before a connection is disconnected.
         */
        private Long maxIdleTimeMillis;

        /**
         * Maximum connection lifetime in milliseconds
         */
        private Long maxLifeTimeMillis;

        public Integer getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(Integer maxConnections) {
            this.maxConnections = (maxConnections == null || maxConnections <= 0) ? DEFAULT_MAX_CONNECTIONS : maxConnections;
        }

        public Long getMaxIdleTimeMillis() {
            return maxIdleTimeMillis;
        }

        public void setMaxIdleTimeMillis(Long maxIdleTimeMillis) {
            this.maxIdleTimeMillis = maxIdleTimeMillis == null ? DEFAULT_MAX_IDLE_TIME_MILLIS : maxIdleTimeMillis;
        }

        public Long getMaxLifeTimeMillis() {
            return maxLifeTimeMillis;
        }

        public void setMaxLifeTimeMillis(Long maxLifeTimeMillis) {
            this.maxLifeTimeMillis = maxLifeTimeMillis == null ? DEFAULT_MAX_LIFE_TIME_MILLIS : maxLifeTimeMillis;
        }
    }

    /**
     * Base URL to be used by {@link WebClient}
     */
    private String baseUrl;

    private int connectionTimeoutMillis;

    private int readTimeoutMillis;

    private int writeTimeoutMillis;

    private boolean useSSL;

    private boolean followRedirects;

    private int sslHandshakeTimeoutMillis;

    /**
     * Maximum in memory size in MB, must be greater than or equal to 256MB.
     */
    private int maxInMemorySizeMB;

    private Long responseTimeoutInMillis;


    private ConnectionPoolSetting pool = new ConnectionPoolSetting();

    /**
     * Base URL for HTTP client.
     * @return
     */
    public String getBaseUrl() {
        return this.baseUrl;
    }

    public void setBaseUrl(String url) {
        Preconditions.checkArgument(StringUtils.isNotBlank(url), "url cannot be blank");
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            Preconditions.checkArgument(e == null, "url is not valid URL");
        }
        this.baseUrl = url;
    }

    /**
     * Timeout in milliseconds before a connection can be established. If 0 or less, use default value.
     * @return
     */
    public int getConnectionTimeoutMillis() {
        return connectionTimeoutMillis < 0 ? DEFAULT_CONNECTION_TIMEOUT_MS : connectionTimeoutMillis;
    }

    public void setConnectionTimeoutMillis(int connectionTimeoutMillis) {
        this.connectionTimeoutMillis = connectionTimeoutMillis < 0 ? DEFAULT_CONNECTION_TIMEOUT_MS : connectionTimeoutMillis;
    }

    /**
     * Read timeout in milliseconds. If 0 or less, use default value.
     * @return
     */
    public int getReadTimeoutMillis() {
        return readTimeoutMillis < 0 ? DEFAULT_READ_TIMEOUT_MS : readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis < 0 ? DEFAULT_READ_TIMEOUT_MS : readTimeoutMillis;
    }

    /**
     * Write timeout in milliseconds. If 0 or less, use default value.
     * @return
     */
    public int getWriteTimeoutMillis() {
        return writeTimeoutMillis < 0 ? DEFAULT_WRITE_TIMEOUT_MS : writeTimeoutMillis;
    }

    public void setWriteTimeoutMillis(int writeTimeoutMillis) {
        this.writeTimeoutMillis = writeTimeoutMillis < 0 ? DEFAULT_WRITE_TIMEOUT_MS : writeTimeoutMillis;
    }

    /**
     * Is using SSL?
     * @return true if yes, false otherwise
     */
    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    /**
     * Connection pool settings
     * @return {@link ConnectionPoolSetting}
     */
    public ConnectionPoolSetting getPool() {
        return pool == null ? new ConnectionPoolSetting() : pool;
    }

    public void setPool(ConnectionPoolSetting pool) {
        this.pool = pool == null ? new ConnectionPoolSetting() : pool;
    }

    public int getMaxInMemorySizeMB() {
        return maxInMemorySizeMB;
    }

    public void setMaxInMemorySizeMB(int maxInMemorySizeMB) {
        this.maxInMemorySizeMB = maxInMemorySizeMB < DEFAULT_MAX_IN_MEMORY_SIZE_MB ? DEFAULT_MAX_IN_MEMORY_SIZE_MB : maxInMemorySizeMB;
    }

    public Long getResponseTimeoutInMillis() {
        return responseTimeoutInMillis;
    }

    public void setResponseTimeoutInMillis(Long responseTimeoutInMillis) {
        this.responseTimeoutInMillis = responseTimeoutInMillis;
    }

    public int getSslHandshakeTimeoutMillis() {
        return sslHandshakeTimeoutMillis <= 0 ? DEFAULT_HANDSHAKE_TIMEOUT_MS : sslHandshakeTimeoutMillis;
    }

    public void setSslHandshakeTimeoutMillis(int sslHandshakeTimeoutMillis) {
        this.sslHandshakeTimeoutMillis = sslHandshakeTimeoutMillis <= 0 ? DEFAULT_HANDSHAKE_TIMEOUT_MS : sslHandshakeTimeoutMillis;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }
}
