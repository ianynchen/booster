package io.github.booster.factories;

import arrow.core.Option;
import com.google.common.base.Preconditions;
import io.github.booster.commons.circuit.breaker.CircuitBreakerConfig;
import io.github.booster.commons.metrics.MetricsRegistry;
import io.github.booster.commons.retry.RetryConfig;
import io.github.booster.config.thread.ThreadPoolConfigGeneric;
import io.github.booster.http.client.HttpClient;
import io.github.booster.http.client.config.HttpClientConnectionConfig;
import io.github.booster.http.client.request.HttpClientRequestContext;
import io.github.booster.task.Task;
import io.github.booster.task.TaskExecutionContext;
import io.github.booster.task.impl.AsyncTask;
import io.github.booster.task.impl.RequestHandlers;
import io.github.booster.task.impl.SynchronousTask;
import kotlin.jvm.functions.Function1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class TaskFactory {

    private static final Logger log = LoggerFactory.getLogger(TaskFactory.class);

    private final ThreadPoolConfigGeneric threadPoolConfig;

    private final RetryConfig retryConfig;

    private final CircuitBreakerConfig circuitBreakerConfig;

    private final HttpClientFactory httpClientFactory;

    private final MetricsRegistry registry;

    private final Map<String, Task> simpleTasks = new HashMap<>();

    private final Map<String, Task> httpClientTasks = new HashMap<>();

    public TaskFactory(
        ThreadPoolConfigGeneric threadPoolConfig,
        RetryConfig retryConfig,
        CircuitBreakerConfig circuitBreakerConfig,
        HttpClientFactory httpClientFactory,
        MetricsRegistry registry
    ) {
        this.threadPoolConfig = threadPoolConfig;
        this.registry = registry;
        this.retryConfig = retryConfig;
        this.circuitBreakerConfig = circuitBreakerConfig;
        this.httpClientFactory = httpClientFactory;
    }

    /**
     * Default task request exception handler. If
     * no exception handler is specified, use this
     * @param t {@link Throwable} form task input
     * @return default response object, or throws a {@link RuntimeException}
     * @param <Response> type of response object.
     */
    private static <Response> Response handleException(Throwable t) {
        throw new IllegalArgumentException(t);
    }

    /**
     * Creates an {@link AsyncTask}
     * @param name name of the task to create. the name is also used
     *             to look for thread pools, retry settings and
     *             circuit breaker settings.
     * @param processor the task processor that handles inputs and product outputs.
     * @param exceptionHandler exception handler. if missing, a default one is used to
     *                         just throw any exception in the input.
     * @return a {@link Task}
     * @param <Request> input type
     * @param <Response> output type
     */
    protected <Request, Response> Task<Request, Response> createAsyncTask(
            String name,
            Function1<Request, Mono<Option<Response>>> processor,
            Function1<Throwable, Option<Response>> exceptionHandler
    ) {
        if (exceptionHandler == null) {
            exceptionHandler = TaskFactory::handleException;
        }
        return new AsyncTask<>(
                name,
                new RequestHandlers<>(
                        Option.fromNullable(null),
                        Option.fromNullable(exceptionHandler)
                ),
                new TaskExecutionContext(
                        this.threadPoolConfig.tryGet(name),
                        this.retryConfig.tryGet(name),
                        this.circuitBreakerConfig.tryGet(name),
                        this.registry
                ),
                processor
        );
    }

    /**
     * Creates an {@link SynchronousTask}
     * @param name name of the task to create. the name is also used
     *             to look for thread pools, retry settings and
     *             circuit breaker settings.
     * @param processor the task processor that handles inputs and product outputs.
     * @param exceptionHandler exception handler. if missing, a default one is used to
     *                         just throw any exception in the input.
     * @return a {@link Task}
     * @param <Request> input type
     * @param <Response> output type
     */
    protected <Request, Response> Task<Request, Response> createSyncTask(
            String name,
            Function1<Request, Option<Response>> processor,
            Function1<Throwable, Option<Response>> exceptionHandler
    ) {
        if (exceptionHandler == null) {
            exceptionHandler = TaskFactory::handleException;
        }
        return new SynchronousTask<>(
                name,
                new RequestHandlers<>(
                        Option.fromNullable(null),
                        Option.fromNullable(exceptionHandler)
                ),
                new TaskExecutionContext(
                        this.threadPoolConfig.tryGet(name),
                        this.retryConfig.tryGet(name),
                        this.circuitBreakerConfig.tryGet(name),
                        this.registry
                ),
                processor
        );
    }

    /**
     * Default input exception handler. If input comes with
     * exception, this handler just returns a PRECONDITION_FAILED error
     * as client response.
     * @param t {@link Throwable} in the input
     * @return {@link ResponseEntity} with PRECONDITION_FAILED as status code.
     * @param <Response> response type.
     */
    private static <Response> Option<ResponseEntity<Response>> handleRequestException(Throwable t) {
        return Option.fromNullable(
                ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build()
        );
    }

    /**
     * Creates a {@link Task} with {@link HttpClient} inside as processor.
     * @param name  name of the http client. The name is used to search for
     *              {@link RetryConfig}, {@link CircuitBreakerConfig}, and {@link ThreadPoolConfigGeneric}
     * @param httpClient {@link HttpClient} wrapped inside the {@link Task}
     * @param exceptionHandler exception handler for input exceptions.
     * @return {@link Task} with {@link HttpClient} wrapped inside.
     * @param <Request> type of request object
     * @param <Response> type of response object.
     */
    protected <Request, Response> Task<HttpClientRequestContext<Request, Response>, ResponseEntity<Response>> createHttpClientTask(
            String name,
            HttpClient<Request, Response> httpClient,
            Function1<Throwable, Option<ResponseEntity<Response>>> exceptionHandler
    ) {
        Function1<HttpClientRequestContext<Request, Response>, Mono<Option<ResponseEntity<Response>>>> function =
                request -> httpClient.invoke(request)
                        .map(Option::fromNullable);

        if (exceptionHandler == null) {
            exceptionHandler = TaskFactory::handleRequestException;
        }
        return new AsyncTask<>(
                name,
                new RequestHandlers<>(
                        Option.fromNullable(null),
                        Option.fromNullable(exceptionHandler)
                ),
                new TaskExecutionContext(
                        this.threadPoolConfig.tryGet(name),
                        this.retryConfig.tryGet(name),
                        this.circuitBreakerConfig.tryGet(name),
                        this.registry
                ),
                function
        );
    }

    /**
     * Retries a {@link Task} with {@link HttpClient} inside as processor from cache, if not
     * available in cache, creates it.
     * @param name  name of the http client. The name is used to search for {@link HttpClientConnectionConfig}
     *              {@link RetryConfig}, {@link CircuitBreakerConfig}, and {@link ThreadPoolConfigGeneric}
     * @return {@link Task} with {@link HttpClient} wrapped inside.
     * @param <Request> type of request object
     * @param <Response> type of response object.
     */
    public <Request, Response> Task<HttpClientRequestContext<Request, Response>, ResponseEntity<Response>> getHttpTask(
            String name
    ) {
        return this.getHttpTask(name, null);
    }

    /**
     * Retries a {@link Task} with {@link HttpClient} inside as processor from cache, if not
     * available in cache, creates it.
     * @param name  name of the http client. The name is used to search for {@link HttpClientConnectionConfig}
     *              {@link RetryConfig}, {@link CircuitBreakerConfig}, and {@link ThreadPoolConfigGeneric}
     * @param exceptionHandler input exception handler, if null, uses default one.
     * @return {@link Task} with {@link HttpClient} wrapped inside.
     * @param <Request> type of request object
     * @param <Response> type of response object.
     */
    public <Request, Response> Task<HttpClientRequestContext<Request, Response>, ResponseEntity<Response>> getHttpTask(
            String name,
            Function1<Throwable, Option<ResponseEntity<Response>>> exceptionHandler
    ) {
        synchronized (this.httpClientTasks) {
            if (this.httpClientTasks.containsKey(name)) {
                log.debug("booster-starter - http task already exists for: [{}]", name);
                return this.httpClientTasks.get(name);
            } else {
                log.debug("booster-starter - creating http task for: [{}]", name);
                HttpClient<Request, Response> httpClient = this.httpClientFactory.get(name);
                Preconditions.checkNotNull(httpClient, "HTTP client cannot be null");
                Task<HttpClientRequestContext<Request, Response>, ResponseEntity<Response>> task =
                        this.createHttpClientTask(name, httpClient, exceptionHandler);
                Preconditions.checkNotNull(task, "task cannot be null");
                this.httpClientTasks.put(name, task);
                return task;
            }
        }
    }

    /**
     * Retrieves an asynchronous {@link Task}. If not present, attempts to
     * create it first.
     * @param name name of the task. The name is also used to look for {@link ThreadPoolConfigGeneric},
     *             {@link RetryConfig} and {@link CircuitBreakerConfig}
     * @param processor asynchronous processor for the task.
     * @return {@link Task}
     * @param <Request> request object type
     * @param <Response> response object type
     */
    public <Request, Response> Task<Request, Response> getAsyncTask(
            String name,
            Function1<Request, Mono<Option<Response>>> processor
    ) {
        return this.getAsyncTask(name, processor, null);
    }

    /**
     * Retrieves an asynchronous {@link Task}. If not present, attempts to
     * create it first.
     * @param name name of the task. The name is also used to look for {@link ThreadPoolConfigGeneric},
     *             {@link RetryConfig} and {@link CircuitBreakerConfig}
     * @param processor asynchronous processor for the task.
     * @param exceptionHandler input exception handler. if null, a default one is used.
     * @return {@link Task}
     * @param <Request> request object type
     * @param <Response> response object type
     */
    public <Request, Response> Task<Request, Response> getAsyncTask(
            String name,
            Function1<Request, Mono<Option<Response>>> processor,
            Function1<Throwable, Option<Response>> exceptionHandler
    ) {
        synchronized (this.simpleTasks) {
            if (this.simpleTasks.containsKey(name)) {
                log.debug("booster-starter - async task already exists for: [{}]", name);
                return (Task<Request, Response>) this.simpleTasks.get(name);
            } else {
                log.debug("booster-starter - creating async task for: [{}]", name);
                Task<Request, Response> task = this.createAsyncTask(name, processor, exceptionHandler);
                this.simpleTasks.put(name, task);
                return task;
            }
        }
    }

    /**
     * Retrieves an synchronous {@link Task}. If not present, attempts to
     * create it first.
     * @param name name of the task. The name is also used to look for {@link ThreadPoolConfigGeneric},
     *             {@link RetryConfig} and {@link CircuitBreakerConfig}
     * @param processor synchronous processor for the task.
     * @return {@link Task}
     * @param <Request> request object type
     * @param <Response> response object type
     */
    public <Request, Response> Task<Request, Response> getSyncTask(
            String name,
            Function1<Request, Option<Response>> processor
    ) {
        return this.getSyncTask(name, processor, null);
    }

    /**
     * Retrieves an synchronous {@link Task}. If not present, attempts to
     * create it first.
     * @param name name of the task. The name is also used to look for {@link ThreadPoolConfigGeneric},
     *             {@link RetryConfig} and {@link CircuitBreakerConfig}
     * @param processor synchronous processor for the task.
     * @param exceptionHandler input exception handler. if null, a default one is used.
     * @return {@link Task}
     * @param <Request> request object type
     * @param <Response> response object type
     */
    public <Request, Response> Task<Request, Response> getSyncTask(
            String name,
            Function1<Request, Option<Response>> processor,
            Function1<Throwable, Option<Response>> exceptionHandler
    ) {
        synchronized (this.simpleTasks) {
            if (this.simpleTasks.containsKey(name)) {
                log.debug("booster-starter - sync task already exists for: [{}]", name);
                return (Task<Request, Response>) this.simpleTasks.get(name);
            } else {
                log.debug("booster-starter - creating sync task for: [{}]", name);
                Task<Request, Response> task = this.createSyncTask(name, processor, exceptionHandler);
                this.simpleTasks.put(name, task);
                return task;
            }
        }
    }
}
