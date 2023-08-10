package io.github.booster.factories

import arrow.core.Option
import arrow.core.Option.Companion.fromNullable
import com.google.common.base.Preconditions
import io.github.booster.commons.circuit.breaker.CircuitBreakerConfig
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.commons.retry.RetryConfig
import io.github.booster.config.thread.ThreadPoolConfig
import io.github.booster.http.client.HttpClient
import io.github.booster.http.client.request.HttpClientRequestContext
import io.github.booster.task.Task
import io.github.booster.task.TaskExecutionContext
import io.github.booster.task.impl.AsyncTask
import io.github.booster.task.impl.RequestHandlers
import io.github.booster.task.impl.SynchronousTask
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono

class TaskFactory(
    private val threadPoolConfig: ThreadPoolConfig,
    private val retryConfig: RetryConfig,
    private val circuitBreakerConfig: CircuitBreakerConfig,
    private val httpClientFactory: HttpClientFactory,
    private val registry: MetricsRegistry
) {
    private val simpleTasks: MutableMap<String, Task<*, *>> = HashMap()
    private val httpClientTasks: MutableMap<String, Task<*, *>> = HashMap()

    /**
     * Creates an [AsyncTask]
     * @param name name of the task to create. the name is also used
     * to look for thread pools, retry settings and
     * circuit breaker settings.
     * @param processor the task processor that handles inputs and product outputs.
     * @param exceptionHandler exception handler. if missing, a default one is used to
     * just throw any exception in the input.
     * @return a [Task]
     * @param <Request> input type
     * @param <Response> output type
    </Response></Request> */
    private fun <Request, Response> createAsyncTask(
        name: String,
        processor: Function1<Request, Mono<Option<Response>>>,
        exceptionHandler: Function1<Throwable, Option<Response>>?
    ): Task<Request, Response> {

        return AsyncTask(
            name,
            RequestHandlers(
                fromNullable(null),
                fromNullable(exceptionHandler)
            ),
            TaskExecutionContext(
                threadPoolConfig.tryGet(name),
                retryConfig.tryGet(name),
                circuitBreakerConfig.tryGet(name),
                registry
            ),
            processor
        )
    }

    /**
     * Creates an [SynchronousTask]
     * @param name name of the task to create. the name is also used
     * to look for thread pools, retry settings and
     * circuit breaker settings.
     * @param processor the task processor that handles inputs and product outputs.
     * @param exceptionHandler exception handler. if missing, a default one is used to
     * just throw any exception in the input.
     * @return a [Task]
     * @param <Request> input type
     * @param <Response> output type
    </Response></Request> */
    private fun <Request, Response> createSyncTask(
        name: String,
        processor: Function1<Request, Option<Response>>,
        exceptionHandler: Function1<Throwable, Option<Response>>?
    ): Task<Request, Response> {

        return SynchronousTask(
            name,
            RequestHandlers(
                fromNullable(null),
                fromNullable(exceptionHandler)
            ),
            TaskExecutionContext(
                threadPoolConfig.tryGet(name),
                retryConfig.tryGet(name),
                circuitBreakerConfig.tryGet(name),
                registry
            ),
            processor
        )
    }

    /**
     * Creates a [Task] with [HttpClient] inside as processor.
     * @param name  name of the http client. The name is used to search for
     * [RetryConfig], [CircuitBreakerConfig], and [ThreadPoolConfig]
     * @param httpClient [HttpClient] wrapped inside the [Task]
     * @param exceptionHandler exception handler for input exceptions.
     * @return [Task] with [HttpClient] wrapped inside.
     * @param <Request> type of request object
     * @param <Response> type of response object.
    </Response></Request> */
    private fun <Request, Response> createHttpClientTask(
        name: String,
        httpClient: HttpClient<Request, Response>,
        exceptionHandler: Function1<Throwable, Option<ResponseEntity<Response>>>?
    ): Task<HttpClientRequestContext<Request, Response>, ResponseEntity<Response>> {

        val function: Function1<HttpClientRequestContext<Request, Response>, Mono<Option<ResponseEntity<Response>>>> =
            { request: HttpClientRequestContext<Request, Response> ->
                httpClient.invoke(request)
                    .map { a: ResponseEntity<Response> ->
                        fromNullable(a)
                    }
            }

        return AsyncTask(
            name,
            RequestHandlers(
                fromNullable(null),
                fromNullable(exceptionHandler)
            ),
            TaskExecutionContext(
                threadPoolConfig.tryGet(name),
                retryConfig.tryGet(name),
                circuitBreakerConfig.tryGet(name),
                registry
            ),
            function
        )
    }

    /**
     * Retries a [Task] with [HttpClient] inside as processor from cache, if not
     * available in cache, creates it.
     * @param name  name of the http client. The name is used to search for [HttpClientConnectionConfig]
     * [RetryConfig], [CircuitBreakerConfig], and [ThreadPoolConfig]
     * @return [Task] with [HttpClient] wrapped inside.
     * @param <Request> type of request object
     * @param <Response> type of response object.
    </Response></Request> */
    fun <Request, Response> getHttpTask(
        name: String
    ): Task<HttpClientRequestContext<Request, Response>, ResponseEntity<Response>> {
        return this.getHttpTask(name, null)
    }

    /**
     * Retries a [Task] with [HttpClient] inside as processor from cache, if not
     * available in cache, creates it.
     * @param name  name of the http client. The name is used to search for [HttpClientConnectionConfig]
     * [RetryConfig], [CircuitBreakerConfig], and [ThreadPoolConfig]
     * @param exceptionHandler input exception handler, if null, uses default one.
     * @return [Task] with [HttpClient] wrapped inside.
     * @param <Request> type of request object
     * @param <Response> type of response object.
    </Response></Request> */
    fun <Request, Response> getHttpTask(
        name: String,
        exceptionHandler: Function1<Throwable, Option<ResponseEntity<Response>>>?
    ): Task<HttpClientRequestContext<Request, Response>, ResponseEntity<Response>> {

        synchronized(httpClientTasks) {
            return if (httpClientTasks.containsKey(name)) {
                log.debug(
                    "booster-starter - http task already exists for: [{}]",
                    name
                )
                httpClientTasks[name] as Task<HttpClientRequestContext<Request, Response>, ResponseEntity<Response>>
            } else {
                log.debug(
                    "booster-starter - creating http task for: [{}]",
                    name
                )
                val httpClient: HttpClient<Request, Response>? =
                    httpClientFactory.get(name) as? HttpClient<Request, Response>
                Preconditions.checkNotNull(
                    httpClient,
                    "HTTP client cannot be null"
                )
                val task = createHttpClientTask(name, httpClient!!, exceptionHandler)
                Preconditions.checkNotNull(
                    task,
                    "task cannot be null"
                )
                httpClientTasks[name] = task
                task
            }
        }
    }

    /**
     * Retrieves an asynchronous [Task]. If not present, attempts to
     * create it first.
     * @param name name of the task. The name is also used to look for [ThreadPoolConfig],
     * [RetryConfig] and [CircuitBreakerConfig]
     * @param processor asynchronous processor for the task.
     * @return [Task]
     * @param <Request> request object type
     * @param <Response> response object type
    </Response></Request> */
    fun <Request, Response> getAsyncTask(
        name: String,
        processor: Function1<Request, Mono<Option<Response>>>
    ): Task<Request, Response> {
        return this.getAsyncTask(name, processor, null)
    }

    /**
     * Retrieves an asynchronous [Task]. If not present, attempts to
     * create it first.
     * @param name name of the task. The name is also used to look for [ThreadPoolConfig],
     * [RetryConfig] and [CircuitBreakerConfig]
     * @param processor asynchronous processor for the task.
     * @param exceptionHandler input exception handler. if null, a default one is used.
     * @return [Task]
     * @param <Request> request object type
     * @param <Response> response object type
    </Response></Request> */
    fun <Request, Response> getAsyncTask(
        name: String,
        processor: Function1<Request, Mono<Option<Response>>>,
        exceptionHandler: Function1<Throwable, Option<Response>>?
    ): Task<Request, Response> {
        synchronized(simpleTasks) {
            return if (simpleTasks.containsKey(name)) {
                log.debug(
                    "booster-starter - async task already exists for: [{}]",
                    name
                )
                simpleTasks[name] as Task<Request, Response>
            } else {
                log.debug(
                    "booster-starter - creating async task for: [{}]",
                    name
                )
                val task = createAsyncTask(name, processor, exceptionHandler)
                simpleTasks[name] = task
                task
            }
        }
    }

    /**
     * Retrieves an synchronous [Task]. If not present, attempts to
     * create it first.
     * @param name name of the task. The name is also used to look for [ThreadPoolConfig],
     * [RetryConfig] and [CircuitBreakerConfig]
     * @param processor synchronous processor for the task.
     * @return [Task]
     * @param <Request> request object type
     * @param <Response> response object type
    </Response></Request> */
    fun <Request, Response> getSyncTask(
        name: String,
        processor: Function1<Request, Option<Response>>
    ): Task<Request, Response> {
        return this.getSyncTask(name, processor, null)
    }

    /**
     * Retrieves an synchronous [Task]. If not present, attempts to
     * create it first.
     * @param name name of the task. The name is also used to look for [ThreadPoolConfig],
     * [RetryConfig] and [CircuitBreakerConfig]
     * @param processor synchronous processor for the task.
     * @param exceptionHandler input exception handler. if null, a default one is used.
     * @return [Task]
     * @param <Request> request object type
     * @param <Response> response object type
    </Response></Request> */
    fun <Request, Response> getSyncTask(
        name: String,
        processor: Function1<Request, Option<Response>>,
        exceptionHandler: Function1<Throwable, Option<Response>>?
    ): Task<Request, Response> {
        synchronized(simpleTasks) {
            return if (simpleTasks.containsKey(name)) {
                log.debug(
                    "booster-starter - sync task already exists for: [{}]",
                    name
                )
                simpleTasks[name] as Task<Request, Response>
            } else {
                log.debug(
                    "booster-starter - creating sync task for: [{}]",
                    name
                )
                val task = createSyncTask(name, processor, exceptionHandler)
                simpleTasks[name] = task
                task
            }
        }
    }

    companion object {

        private val log = LoggerFactory.getLogger(TaskFactory::class.java)
    }
}
