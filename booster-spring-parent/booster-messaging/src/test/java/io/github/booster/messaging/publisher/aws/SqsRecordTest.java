package io.github.booster.messaging.publisher.aws;

import arrow.core.Either;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.booster.messaging.config.OpenTelemetryConfig;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqsRecordTest {

    @Test
    void shouldCreate() {
        assertThat(
                SqsRecord.createMessage(
                        null,
                        "test"
                ),
                notNullValue()
        );
        assertThat(
                SqsRecord.createMessage(
                        Map.of("test", "test"),
                        "test"
                ),
                notNullValue()
        );
        assertThat(
                SqsRecord.createMessage(
                        null,
                        null,
                        null,
                        "test"
                ),
                notNullValue()
        );
        assertThat(
                SqsRecord.createMessage(
                        null,
                        null,
                        Map.of("test", "test"),
                        "test"
                ),
                notNullValue()
        );
        assertThat(
                SqsRecord.createMessage(
                        "test",
                        "test",
                        Map.of("test", "test"),
                        "test"
                ),
                notNullValue()
        );
    }

    @Test
    void shouldFailCreate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SqsRecord.createMessage(
                        "test",
                        null,
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> SqsRecord.createMessage(
                        null,
                        "test",
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> SqsRecord.createMessage(
                        "",
                        "test",
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> SqsRecord.createMessage(
                        " ",
                        "test",
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> SqsRecord.createMessage(
                        "test",
                        "",
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> SqsRecord.createMessage(
                        "test",
                        " ",
                        null,
                        "test"
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> SqsRecord.createMessage(
                        "test",
                        "test",
                        null,
                        null
                )
        );
    }

    @Test
    void shouldCreateSendMessageRequest() {
        SqsRecord<String> record = SqsRecord.createMessage(
                null,
                null,
                Map.of("test", "test"),
                "test"
        );

        Either<Throwable, SendMessageRequest> request = record.createSendMessageRequest(
                "http://test",
                new ObjectMapper(),
                new OpenTelemetryConfig(null, "test"),
                false
        );
        assertThat(request, notNullValue());
        assertThat(request.isRight(), equalTo(true));
        assertThat(request.getOrNull(), notNullValue());
    }

    @Test
    void shouldCreateSendMessageRequestWithGroup() {
        SqsRecord<String> record = SqsRecord.createMessage(
                "group",
                "dedupe",
                Map.of("test", "test"),
                "test"
        );

        Either<Throwable, SendMessageRequest> request = record.createSendMessageRequest(
                "http://test",
                new ObjectMapper(),
                new OpenTelemetryConfig(null, "test"),
                false
        );
        assertThat(request, notNullValue());
        assertThat(request.isRight(), equalTo(true));
        assertThat(request.getOrNull(), notNullValue());
        assertThat(request.getOrNull().messageGroupId(), equalTo("group"));
        assertThat(request.getOrNull().messageDeduplicationId(), equalTo("dedupe"));
    }
}
