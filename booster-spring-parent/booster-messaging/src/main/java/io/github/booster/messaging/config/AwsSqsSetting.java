package io.github.booster.messaging.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.net.URI;

/**
 * SQS Setting for SQS sender and receiver
 */
@Data
public class AwsSqsSetting {

    /**
     * Credential setting
     */
    @Data
    public static class AwsCredentials {

        /**
         * Access key
         */
        private String accessKey;

        /**
         * secret key
         */
        private String secretKey;

        /**
         * Default constructor
         */
        public AwsCredentials() {
        }
    }

    /**
     * AWS SQS receiver setting
     */
    @Getter
    public static class ReceiverSetting {

        /**
         * Receives all attributes. By default, not all attributes are sent to receiver
         */
        public static final String ALL_ATTRIBUTES = "All";

        /**
         * Maximum number of messages to receive
         */
        private int maxNumberOfMessages = 1;

        /**
         * Visibility timeout setting
         */
        @Setter
        private Integer visibilityTimeout;

        /**
         * Wait time in seconds
         */
        @Setter
        private Integer waitTimeSeconds;

        /**
         * default constructor
         */
        public ReceiverSetting() {
        }

        /**
         * Sets maximum number of messages to receive
         * @param maxNumberOfMessages maximum number of messages to receive
         */
        public void setMaxNumberOfMessages(int maxNumberOfMessages) {
            if (maxNumberOfMessages < 1) {
                this.maxNumberOfMessages = 1;
            } else if (maxNumberOfMessages > 10) {
                this.maxNumberOfMessages = 10;
            } else {
                this.maxNumberOfMessages = maxNumberOfMessages;
            }
        }

        /**
         * Creates a {@link ReceiveMessageRequest} to receive SQS messages
         * @param queueUrl queue URL
         * @return {@link ReceiveMessageRequest} request that can be sent to SQS
         */
        public ReceiveMessageRequest createRequest(String queueUrl) {
            ReceiveMessageRequest.Builder builder = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl);

            if (this.getWaitTimeSeconds() != null) {
                builder = builder.waitTimeSeconds(this.getWaitTimeSeconds());
            }
            if (this.getVisibilityTimeout() != null) {
                builder = builder.visibilityTimeout(this.getVisibilityTimeout());
            }
            builder.messageAttributeNames(ALL_ATTRIBUTES);

            return builder.maxNumberOfMessages(this.maxNumberOfMessages)
                    .build();
        }
    }

    /**
     * SQS queue URL
     */
    private String queueUrl;

    /**
     * SQS region
     */
    private Region region;

    /**
     * SQS receiver setting
     */
    private ReceiverSetting receiverSetting = new ReceiverSetting();

    /**
     * SQS credentials
     */
    private AwsCredentials credentials;

    /**
     * default constructor
     */
    public AwsSqsSetting() {
    }

    /**
     * Creates an SQS client from {@link AwsSqsSetting}
     * @return {@link SqsClient}
     */
    public SqsClient createClient() {
        SqsClientBuilder builder = SqsClient.builder()
                .region(this.region)
                .endpointOverride(URI.create(this.queueUrl));

        if (this.credentials != null) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                    this.credentials.accessKey,
                                    this.credentials.secretKey
                            )
                    )
            );
        }
        return builder.build();
    }
}
