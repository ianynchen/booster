package io.github.booster.messaging;

/**
 * Metrics constants used for metrics reporting.
 */
public interface MessagingMetricsConstants {

    String TOPIC = "topic";

    String NAME = "name";

    String STATUS = "status";

    String REASON = "reason";

    String SUCCESS_STATUS = "success";

    String FAILURE_STATUS = "failure";

    String ENQUEUE_TIME = "enqueue_time";

    String ENQUEUE_COUNT = "enqueue_count";

    String DEQUEUE_COUNT = "dequeue_count";

    String DEQUEUE_TIME = "dequeue_time";

    String PROCESS_TIME = "process_time";

    String PROCESS_COUNT = "process_count";

    String SEND_TIME = "send_time";

    String SEND_COUNT = "send_count";

    String SUBSCRIBER_PULL_COUNT = "subscriber_pull_count";

    String SUBSCRIBER_PULL_TIME = "subscriber_pull_time";

    String MESSAGING_TYPE = "messaging_type";

    String KAFKA = "kafka";

    String GCP_PUBSUB = "gcp_pubsub";

    String SUBSCRIBER_PROCESS_COUNT = "";

    String ACKNOWLEDGEMENT_COUNT = "acknowledge_count";
}
