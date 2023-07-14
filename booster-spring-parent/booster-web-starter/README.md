# booster-starter

Booster Starter for Web, HTTP Client and Task

## Getting started

### Learn Each Project 
1. [Booster Task](../../booster-task/README.md)
2. [Booster Web](../booster-web/README.md)
3. [Booster HTTP Client](../booster-http-client/README.md)

### Project Setup

## Features

This starter package integrates the following libraries and provides 
an easy way to put everything together:

1. booster-task, 
2. booster-http-client, and
3. booster-web.

### Creating a RESTful Endpoint

Creating an endpoint is the same as you would with traditional Spring
Web application. Just start with RestController and define your endpoints 
as you normally would. Make sure the endpoints return a Mono<Either<Throwable, Response>>
object. Booster web will automatically detect the return type and format the 
response.

Once the endpoint is created, Springfox documentation is automatically available 
on the endpoints.

### Creating an HTTP Client

There are two ways one can create an HTTP client:

1. Create HTTP client as is via ```HttpClientFactory```, or
2. Create HTTP Client as a ```Task``` via ```TaskFactory```.

There isn't any fundamental difference in the two approaches as they 
both use ```HttpClient```. However, using ```TaskFactory``` makes the 
task of building a robust HTTP call much easier:

1. If retry or circuit breaker is configured, a retry and circuit breaker setting will be picked up and added to the task.
2. If a thread pool is configured with the same name as the task, a thread pool is added to the task for execution.

So using HTTP client task created from ```TaskFactory``` gives:

1. Automatic retries;
2. Automatic circuit breaker capability;
3. Automatically making the HTTP calls in a thread pool;
4. Thread pool metrics and HTTP call metrics are automatically recorded as well.

If the ```HttpClientFactory``` approach is taken, one needs to handle

1. Retries,
2. Circuit breaker,
3. Thread pools, and possibly thread pool metrics reporting 

manually.

## Configuration Sections 

Thread pools, retry settings, circuit breaker settings are implemented using
ConfigurationProperties. The prefixes are:

1. Retry: ```booster.task.retry.settings```
2. Circuit breaker: ```booster.task.circuit-breaker.settings```
3. Thread pool: ```booster.task.threads.settings```
4. HTTP client setting: ```booster.http.client.connection.settings```

## Custom Tags 

Booster starter library also adds custom tags to all metrics reported:
1. a ```service``` tag, with the name of service, extracted from ```spring.application.name``` property.
2. a ```environment``` tag, extracted from ```spring.profiles.active```

By default, service tag takes value of **dummy** and environment tag takes value of **default**.
Please make sure proper values are set for these two properties to have better reporting.

## Example 

Take a look at [BoosterSampleApplication](src/test/java/io/github/booster/config/example/BoosterSampleApplication.java)
