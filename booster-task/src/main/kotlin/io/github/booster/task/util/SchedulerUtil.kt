package io.github.booster.task.util

import arrow.core.Option
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.util.concurrent.ExecutorService

/**
 * Converts optional executor service to optional scheduler.
 */
fun toScheduler(executorServiceOption: Option<ExecutorService>): Option<Scheduler> =
    executorServiceOption.map { executorService -> Schedulers.fromExecutorService(executorService) }
