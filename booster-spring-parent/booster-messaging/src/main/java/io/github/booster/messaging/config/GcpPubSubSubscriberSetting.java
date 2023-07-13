package io.github.booster.messaging.config;

import com.google.api.client.util.Preconditions;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

/**
 * GCP subscriber setting. A subscriber pulls from
 * a pub/sub subscription and processes messages received.
 */
@Getter
@ToString
@EqualsAndHashCode
public class GcpPubSubSubscriberSetting {

    private String subscription;

    private int maxRecords;

    /**
     * Sets subscription the subscriber needs to pull from
     * @param subscription pub/sub subscription
     */
    public void setSubscription(String subscription) {
        Preconditions.checkArgument(StringUtils.isNotBlank(subscription), "subscription cannot be blank");
        this.subscription = subscription;
    }

    /**
     * Sets maximum records to be pulled in a single pull operation. The
     * subscriber will pull up to this number of records, but will return
     * immediately if not enough records are pulled.
     * @param maxRecords maximum records to be pulled.
     */
    public void setMaxRecords(int maxRecords) {
        Preconditions.checkArgument(maxRecords > 0, "max records cannot be 0 or below 0");
        this.maxRecords = maxRecords;
    }
}
