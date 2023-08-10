package io.github.booster.factories

import arrow.core.Either
import arrow.core.Option
import arrow.core.Option.Companion.fromNullable
import arrow.core.getOrElse
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.booster.commons.circuit.breaker.CircuitBreakerConfig
import io.github.booster.commons.circuit.breaker.CircuitBreakerSetting
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.commons.retry.RetryConfig
import io.github.booster.commons.retry.RetrySetting
import io.github.booster.config.BoosterConfig
import io.github.booster.config.example.BoosterSampleApplication
import io.github.booster.config.example.dto.GreetingResponse
import io.github.booster.config.thread.ThreadPoolConfig
import io.github.booster.config.thread.ThreadPoolSetting
import io.github.booster.http.client.HttpClient
import io.github.booster.http.client.config.HttpClientConnectionConfig
import io.github.booster.http.client.config.HttpClientConnectionSetting
import io.github.booster.http.client.request.HttpClientRequestContext
import io.github.booster.task.Task
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.sameInstance
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Assert.assertThrows
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@ExtendWith(MockitoExtension::class)
@SpringBootTest(classes = [BoosterSampleApplication::class, BoosterConfig::class])
internal class TaskFactoryTest {
    @Autowired
    private val httpClientConnectionConfig: HttpClientConnectionConfig? = null
    private var threadPoolConfig: ThreadPoolConfig? = null
    private var retryConfig: RetryConfig? = null
    private var circuitBreakerConfig: CircuitBreakerConfig? = null
    private val mapper = ObjectMapper()
    private val registry = MetricsRegistry(SimpleMeterRegistry())
    private var factory: TaskFactory? = null
    private var factoryWithMock: TaskFactory? = null

    @Mock
    private var mockHttpClientFactory: HttpClientFactory? = null
    private var mockHttpClient: HttpClient<GreetingResponse, GreetingResponse>? = null
    @BeforeEach
    fun setup() {
        val setting = HttpClientConnectionSetting()
        setting.baseUrl = "http://www.ibm.com"
        httpClientConnectionConfig!!.setSettings(mapOf(Pair("client", setting)))
        threadPoolConfig = ThreadPoolConfig()
        threadPoolConfig!!.setSettings(
            mapOf(
                Pair("async", ThreadPoolSetting(null, null, null, null)),
                Pair("sync", ThreadPoolSetting(null, null, null, null)),
                Pair("client", ThreadPoolSetting(null, null, null, null))
            )
        )
        threadPoolConfig!!.setMetricsRegistry(registry)
        retryConfig = RetryConfig()
        retryConfig!!.setSettings(
            mapOf(Pair("client", RetrySetting()))
        )
        circuitBreakerConfig = CircuitBreakerConfig()
        circuitBreakerConfig!!.setSettings(
            mapOf(Pair("client", CircuitBreakerSetting()))
        )
        factory = TaskFactory(
            threadPoolConfig!!,
            retryConfig!!,
            circuitBreakerConfig!!,
            HttpClientFactory(httpClientConnectionConfig, WebClient.builder(), mapper),
            registry
        )
    }

    @Test
    fun shouldCreateAsyncTask() {
        assertThat(factory, Matchers.notNullValue())
        val task = factory!!.getAsyncTask(
            "async"
        ) { str: String -> Mono.just(fromNullable(str.length)) }
        val task2 = factory!!.getAsyncTask(
            "async2"
        ) { str: String -> Mono.just(fromNullable(str.length)) }
        val task3 = factory!!.getAsyncTask(
            "async"
        ) { str: String -> Mono.just(fromNullable(str.length)) }
        assertThat(task, Matchers.notNullValue())
        assertThat(task2, Matchers.notNullValue())
        assertThat(task3, Matchers.notNullValue())
        assertThat(task, Matchers.sameInstance(task3))
        assertThat(task, Matchers.not(Matchers.sameInstance(task2)))
        StepVerifier.create(task.execute("hello"))
            .consumeNextWith { either: Either<Throwable, Option<Int>> ->
                assertThat(either.isRight(), equalTo(true))
                val value =
                    either.getOrElse { o: Throwable -> fromNullable(0) }
                assertThat(value.orNull(), Matchers.`is`("hello".length))
            }.verifyComplete()
    }

    @Test
    fun shouldCreateSyncTask() {
        assertThat(factory, notNullValue())
        val task = factory!!.getSyncTask(
            "async"
        ) { str: String -> fromNullable(str.length) }
        val task2 = factory!!.getSyncTask(
            "async2"
        ) { str: String -> fromNullable(str.length) }
        val task3 = factory!!.getSyncTask(
            "async"
        ) { str: String -> fromNullable(str.length) }
        assertThat(task, notNullValue())
        assertThat(task2, notNullValue())
        assertThat(task3, notNullValue())
        assertThat(task, sameInstance(task3))
        assertThat(task, not(sameInstance(task2)))

        StepVerifier.create(task.execute("hello"))
            .consumeNextWith { either: Either<Throwable, Option<Int>> ->
                assertThat(either.isRight(), Matchers.`is`(true))
                val value =
                    either.getOrElse { o: Throwable -> fromNullable(0) }
                assertThat(value.orNull(), Matchers.`is`("hello".length))
            }.verifyComplete()
    }

    @Test
    fun shouldReturnResponse() {
        mockHttpClient = mock(HttpClient::class.java) as HttpClient<GreetingResponse, GreetingResponse>

        Mockito.`when`(mockHttpClient!!.invoke(any()))
            .thenReturn(
                Mono.just(
                    ResponseEntity.ok(
                        GreetingResponse("server", "hola")
                    )
                )
            )

        mockHttpClientFactory = mock(HttpClientFactory::class.java)
        Mockito.`when`(mockHttpClientFactory!![anyString()]).thenReturn(mockHttpClient)

        factoryWithMock = TaskFactory(
            threadPoolConfig!!,
            retryConfig!!,
            circuitBreakerConfig!!,
            mockHttpClientFactory!!,
            registry
        )
        val task = factoryWithMock!!.getHttpTask<GreetingResponse, GreetingResponse>("test")
        assertThat(task, Matchers.notNullValue())

        val context = HttpClientRequestContext.builder<GreetingResponse, GreetingResponse>()
            .request(
                GreetingResponse("world", "hello")
            ).responseClass(GreetingResponse::class.java)
            .path("")
            .requestMethod(HttpMethod.GET)
            .build()
        StepVerifier.create(task.execute(context))
            .consumeNextWith { either: Either<Throwable, Option<ResponseEntity<GreetingResponse>>> ->
                assertThat(either.isRight(), Matchers.`is`(true))
                val response = either.getOrNull()!!.orNull()!!.body
                assertThat(response, notNullValue())
                assertThat(response?.from, equalTo("server"))
                assertThat(response?.greeting, equalTo("hola"))
            }.verifyComplete()
    }

    @Test
    fun shouldCreateHttpClientTask() {
        assertThat(factory, notNullValue())
        val task: Task<HttpClientRequestContext<Any, GreetingResponse>, ResponseEntity<GreetingResponse>> =
            factory!!.getHttpTask("client")
        assertThat(task, notNullValue())
        assertThrows(
            IllegalArgumentException::class.java
        ) { factory!!.getHttpTask<Any, Any>("client2") }
        assertThat(
            factory!!.getHttpTask("client"),
            sameInstance<Task<out HttpClientRequestContext<Any, out Any>?, out ResponseEntity<out Any>?>>(task)
        )
        val request = Either.Left(
            IllegalArgumentException()
        )
        StepVerifier.create(
            task.execute(request)
        ).consumeNextWith { either: Either<Throwable, Option<ResponseEntity<GreetingResponse>>> ->
            assertThat(either.isRight(), `is`(false))
        }.verifyComplete()
    }
}
