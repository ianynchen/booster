package io.github.booster.messaging.config;

import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

@Data
public class AwsSqsSetting {

    @Data
    public static class AwsCredentials {

        private String accessKey;

        private String secretKey;
    }

    @Getter
    public static class ReceiverSetting {

        private List<String> attributeNames = List.of();

        private int maxNumberOfMessages = 1;

        private Integer visibilityTimeout;

        private Integer waitTimeSeconds;

        public void setAttributeNames(List<String> attributeNames) {
            this.attributeNames = attributeNames == null ? List.of() : attributeNames;
        }

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

        public ReceiveMessageRequest createRequest(String url) {
            ReceiveMessageRequest.Builder builder = ReceiveMessageRequest.builder();

            return builder.queueUrl(url)
                    .messageAttributeNames(this.attributeNames)
                    .maxNumberOfMessages(this.maxNumberOfMessages)
                    .visibilityTimeout(this.visibilityTimeout)
                    .waitTimeSeconds(this.waitTimeSeconds)
                    .build();
        }
    }

    private String url;

    private Region region;

    private ReceiverSetting receiverSetting;

    private AwsCredentials credentials;

    public SqsClient createClient() {
        SqsClientBuilder builder = SqsClient.builder()
                .region(this.region);

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
