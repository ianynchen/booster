package io.github.booster.factories

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.booster.http.client.config.HttpClientConnectionConfig
import io.github.booster.http.client.config.HttpClientConnectionSetting
import io.github.booster.http.client.impl.HttpClientImpl
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.isA
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.boot.actuate.metrics.web.reactive.client.MetricsWebClientCustomizer
import org.springframework.context.ApplicationContext
import org.springframework.web.reactive.function.client.WebClient

internal class HttpClientFactoryTest {

    @Test
    fun shouldCreate() {
        val bean = mock(MetricsWebClientCustomizer::class.java)
        doNothing().`when`(bean).customize(isA(WebClient.Builder::class.java))

        val context = mock(ApplicationContext::class.java)
        `when`(context.getBean(MetricsWebClientCustomizer::class.java)).thenReturn(bean)

        val config = HttpClientConnectionConfig(context)

        val setting = HttpClientConnectionSetting()
        setting.baseUrl = "http://www.ibm.com"
        config.setSettings(mapOf(Pair("test", setting)))
        val factory = HttpClientFactory(
            config,
            WebClient.builder(),
            ObjectMapper()
        )
        assertThat(factory, notNullValue())
        assertThat(factory["test"], notNullValue())
        assertThat(factory["test"], instanceOf(HttpClientImpl::class.java))
    }
}
