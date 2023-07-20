package io.github.booster.messaging.config;

import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.net.URI;

@Data
public class AwsSqsSetting {

    @Data
    public static class AwsCredentials {

        private String accessKey;

        private String secretKey;
    }

    @Getter
    public static class ReceiverSetting {

        public static final String ALL_ATTRIBUTES = "All";

        private int maxNumberOfMessages = 1;

        private Integer visibilityTimeout;

        private Integer waitTimeSeconds;

        public void setMaxNumberOfMessages(int maxNumberOfMessages) {
            if (maxNumberOfMessages < 1) {
                this.maxNumberOfMessages = 1;
            } else if (maxNumberOfMessages > 10) {
                this.maxNumberOfMessages = 10;
            } else {
                this.maxNumberOfMessages = maxNumberOfMessages;
            }
        }

        public void setVisibilityTimeout(Integer visibilityTimeout) {
            this.visibilityTimeout = visibilityTimeout;
        }

        public void setWaitTimeSeconds(Integer waitTimeSeconds) {
            this.waitTimeSeconds = waitTimeSeconds;
        }

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

    private String queueUrl;

    private Region region;

    private ReceiverSetting receiverSetting = new ReceiverSetting();

    private AwsCredentials credentials;

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
