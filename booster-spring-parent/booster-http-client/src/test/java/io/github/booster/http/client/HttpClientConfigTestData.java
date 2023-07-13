package io.github.booster.http.client;

import io.github.booster.http.client.config.HttpClientConnectionConfig;
import io.github.booster.http.client.config.HttpClientConnectionSetting;
import org.springframework.context.ApplicationContext;

import java.util.Map;

public interface HttpClientConfigTestData {

    static HttpClientConnectionConfig getConfig(ApplicationContext context) {
        HttpClientConnectionSetting setting = new HttpClientConnectionSetting();
        setting.setBaseUrl("http://www.ibm.com");
        setting.setConnectionTimeoutMillis(200);
        setting.setMaxInMemorySizeMB(200);
        setting.setReadTimeoutMillis(200);
        setting.setUseSSL(false);
        setting.setResponseTimeoutInMillis(500L);
        setting.setWriteTimeoutMillis(500);

        HttpClientConnectionSetting.ConnectionPoolSetting poolSetting = new HttpClientConnectionSetting.ConnectionPoolSetting();
        poolSetting.setMaxConnections(200);
        poolSetting.setMaxIdleTimeMillis(200L);
        poolSetting.setMaxLifeTimeMillis(500L);
        setting.setPool(poolSetting);

        HttpClientConnectionConfig config = new HttpClientConnectionConfig(context);
        config.setSettings(Map.of("test", setting));
        return config;
    }
}
