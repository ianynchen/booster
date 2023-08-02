package io.github.booster.messaging;

/**
 * Metrics constants used for metrics reporting.
 */
public interface MessagingMetricsConstants {

    /**
     * topic tag
     */
    String TOPIC = "topic";

    /**
     * name tag
     */
    String NAME = "name";

    /**
     * status tag
     */
    String STATUS = "status";

    /**
     * reason tag
     */
    String REASON = "reason";

    /**
     * success status/reason
     */
    String SUCCESS_STATUS = "success";

    /**
     * generic failure status/reason
     */
    String FAILURE_STATUS = "failure";

    /**
     * enqueue time tag
     */
    String ENQUEUE_TIME = "enqueue_time";

    /**
     * enqueue count tag
     */
    String ENQUEUE_COUNT = "enqueue_count";

    /**
     * dequeue count tag
     */
    String DEQUEUE_COUNT = "dequeue_count";

    /**
     * dequeue time tag
     */
    String DEQUEUE_TIME = "dequeue_time";

    /**
     * publisher send time tag
     */
    String SEND_TIME = "send_time";

    /**
     * publisher send count tag
     */
    String SEND_COUNT = "send_count";

    /**
     * subscriber pull count tag
     */
    String SUBSCRIBER_PULL_COUNT = "subscriber_pull_count";

    /**
     * subscriber pull time tag
     */
    String SUBSCRIBER_PULL_TIME = "subscriber_pull_time";

    /**
     * message type tag, kafka, aws sqs or gcp pub/sub
     */
    String MESSAGING_TYPE = "messaging_type";

    /**
     * message type kafka
     */
    String KAFKA = "kafka";

    /**
     * message type gcp pub/sub
     */
    String GCP_PUBSUB = "gcp_pubsub";

    /**
     * message type aws sqs
     */
    String AWS_SQS = "aws_sqs";

    /**
     * processor process count
     */
    String SUBSCRIBER_PROCESS_COUNT = "subscriber_process_count";

    /**
     * processor acknowledgement count
     */
    String ACKNOWLEDGEMENT_COUNT = "acknowledge_count";
}
