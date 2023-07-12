package io.github.booster.task.util

import arrow.core.Option
import com.google.common.base.Preconditions
import io.github.booster.commons.metrics.MetricsRegistry
import io.github.booster.task.FAILURE
import io.github.booster.task.NAME
import io.github.booster.task.REASON
import io.github.booster.task.STATUS
import io.github.booster.task.SUCCESS
import io.github.booster.task.TASK_EXECUTION_RESULT_COUNT
import io.github.booster.task.TASK_EXECUTION_TIME
import io.micrometer.core.instrument.Timer
import org.slf4j.Logger

/**
 * Record success counter.
 * @param response success response.
 * @param log [Logger]
 * @param registry [MetricsRegistry]
 * @param taskName name of task.
 * @param <T> type of response.
</T> */
fun <T> recordSuccessCount(
    response: T?,
    log: Logger,
    registry: MetricsRegistry,
    taskName: String,
) {
    Preconditions.checkArgument(taskName.isNotBlank(), "task name cannot be blank")
    log.debug("booster-task - task[{}] produced result: [{}]", taskName, response)
    registry.incrementCounter(
        TASK_EXECUTION_RESULT_COUNT,
        NAME, taskName,
        STATUS, SUCCESS,
        REASON, SUCCESS
    )
}

/**
 * Record failure counter.
 * @param t exception to be recorded.
 * @param log [Logger]
 * @param registry [MetricsRegistry]
 * @param taskName name of task.
 */
fun recordFailureCount(
    t: Throwable,
    log: Logger,
    registry: MetricsRegistry,
    taskName: String,
) {
    Preconditions.checkArgument(taskName.isNotBlank(), "task name cannot be blank")
    log.error("booster-task - task[{}] produced exception", taskName, t)
    registry.incrementCounter(
        TASK_EXECUTION_RESULT_COUNT,
        NAME, taskName,
        STATUS, FAILURE,
        REASON, t.javaClass.simpleName
    )
}

/**
 * Record time spent for task execution
 * @param registry [MetricsRegistry]
 * @param sampleOption optional [Timer.Sample]
 * @param taskName name of task.
 */
fun recordTime(
    registry: MetricsRegistry,
    sampleOption: Option<Timer.Sample>,
    taskName: String,
) {
    Preconditions.checkArgument(taskName.isNotBlank(), "task name cannot be blank")
    registry.endSample(
        sampleOption,
        TASK_EXECUTION_TIME,
        NAME, taskName
    )
}
