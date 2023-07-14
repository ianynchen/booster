# Booster Task

Java asynchronous task execution. Part of 
[Booster Project Initiative](https://confluence.lblw.cloud/display/BPF/Booster+Family+Project+Initiative).

Detailed documentation see [Booster Task](https://confluence.lblw.cloud/display/BPF/Booster+Task)

## Purpose
Booster-task supports easy construction of sequential or parallel tasks that can be executed asynchronously.
Asynchronous execution can be challenging at times:
1. Tasks that can be executed in parallel need to be run in different threads;
2. Tasks can generate errors, and logging and recording errors is tedious task;
3. Tasks that fail need sometimes be rerun for error recovery.

Booster-task is designed and implement to address the above issues and provide more:
1. Use threads wisely. Certain tasks do not need to be run in separate threads and avoiding thread creation can reduce memory consumption.
2. Execute tasks in parallel and handles errors and result aggregation gracefully.
3. Generates logs and metrics to provide insight on task execution.
4. Supports retry and circuit breaker natively for error recovery and service protection.

## Get Started 

### Project Setup

### Usage

One can create a task via task constructor. If using Kotlin, every type of task has 
a corresponding builder, e.g. ```AsyncTask``` as ```AsyncTaskBuilder``` which allows one 
to create the corresponding task using DSL.

#### Create A Simple Task

Following example creates a synchronous task to compute string length via constructor.

```kotlin
val completeTask = SynchronousTask<String, Int>(
   "lengthTask",
   emptyThreadPool,
   retryConfig.get("test"),
   circuitBreakerConfig.get("test"),
   registry,
   ::syncLengthFunc,
   ::lengthExceptionThrower
)

task.execute("abc").subscribe()
```

There are two types of simple tasks:
1. Synchronous task - the task processor and corresponding exception handling logic uses synchronous syntax.
2. Asynchronous task - the task processor and corresponding exception handling logic returns Mono either with result or an error.

Simple tasks can be run with their dedicated threads, can have retries or circuit breaker for error 
recovery. A non-blank name is mandatory for simple tasks to be created.

#### Connect Tasks to be Executed in Sequence

To connect two tasks to be executed sequentially:

```kotlin
val task = sequentialTask {
   name("abc")
   registry(io.github.booster.task.registry)
   firstTask(
      syncTask<String, Int?> {
         name("length")
         registry(io.github.booster.task.registry)
         retryOption(retryConfig.get("abc"))
         circuitBreakerOption(circuitBreakerConfig.get("abc"))
         executorOption(threadPool)
         processor {
            it!!.length
         }
      }.build()
   )
   secondTask(
      syncTask<Int, String> {
         name("str")
         registry(io.github.booster.task.registry)
         retryOption(retryConfig.get("abc"))
         circuitBreakerOption(circuitBreakerConfig.get("abc"))
         executorOption(threadPool)
         processor {
            it.toString()
         }
      }.build()
   )
}.build()
```

As each task may already have its dedicated threads, there is no need to provide thread pool for a 
sequential or chained task.

A sequential task also provides a task name based on the two tasks that are connected, hence naming 
a sequential task is not necessary.

Sequential tasks also don't report on execution results, only reports on execution time, as the 
result only depends on the result of the internal tasks, which are already reported.

#### Connect Tasks to be Executed in Parallel

To create a homogeneous parallel task:

```kotlin
        val task = parallelTask {
            name("parallel")
            registry(io.github.booster.task.registry)
            task(
                syncTask<String?, Int> {
                    name("length")
                    registry(io.github.booster.task.registry)
                    retryOption(retryConfig.get("abc"))
                    circuitBreakerOption(circuitBreakerConfig.get("abc"))
                    executorOption(emptyThreadPool)
                    processor {
                        it?.length ?: 0
                    }
                }.build()
            )
        }.build()

        val response = task.execute(Either.Left(IllegalArgumentException()))
```

To create a heterogeneous parallel task:
```kotlin
        val task = tuple3Task {
            name("tuple")
            firstTask(lengthTask)
            secondTask(stringTask)
            thirdTask(lengthTask)
            registry(io.github.booster.task.registry)
            aggregator { either1, either2, either3 ->
                Tuple.of(
                    either1.getOrNull(),
                    either2.getOrNull(),
                    either3.getOrNull()
                )
            }
        }.build()

        val response = task.execute(Either.Right(Tuple.of("abc", 12, "abcd")))
```

Parallel tasks don't have dedicated threads, nor do they have retries or circuit breakers, as parallel 
tasks will only be aggregating results from internal tasks.

Heterogeneous tasks can have up to 8 different input types and 8 different output types.

## Features

There are 4 types of tasks supported:

1. Simple task, this is a task that can be executed in its own thread, and completes a single job.
2. Sequential task, two tasks executed in sequence, the output of the first task is the input for the second task.
3. Parallel task, the input is a list of elements of the same type, and the task executes on each element in the list, produces a list of output elements of the same type.
4. Tuple task, the input consists of a list of elements of different types, and the output elements are of different types as well.
   1. For tuple tasks, each input output type pair is executed in a dedicated task.
   2. The results from each task is then merged together to form the final output.

Of the 4 types of tasks above, only the simple task can be run on dedicated threads. 

### Execution of Tasks in Dedicated Threads 

One only has the option to provide a dedicated thread pool if one creates a simple task, or inherit
from ```AbstractTask```. This is done deliberately.

### Error Recovery 

Error recovery is supported on simple tasks. This type of task represent a single job that needs to 
be completed, and in case of error, error recovery mechanism need to kick in. Currently, two types of 
error recovery are supported:

1. Retry, and 
2. Circuit breaker.

Both implementations use resilience4j to provide implementation. One can provide a ```Retry``` or 
a ```CircuitBreaker``` when creating a ```SimpleTask``` to provide error recovery.

## Metrics Reported
Booster task reports the following metrics for tasks:

| Metric Name         | Type    | Tag    | Tag Values                        | Description                 |
|---------------------|---------|--------|-----------------------------------|-----------------------------|
| task_execution_time | timer   | name   |                                   | task name                   |
| task_result_count   | counter | name   |                                   | task name                   |
|                     |         | status | fail, success                     | execution status            |
|                     |         | reason | success, or exception simple name | reason for execution status |

In addition to the metrics listed above, if a [micrometer](https://micrometer.io/) **MeterRegistry** is provided, 
this will be injected into **Retry** and **CircuitBreaker** objects to allow these objects to report metrics. 
For details about these metrics, refer to [Resilience4j documentation](https://resilience4j.readme.io/docs/micrometer).

Thread pools used by tasks are also monitored by micrometer and report metrics on thread usage.
