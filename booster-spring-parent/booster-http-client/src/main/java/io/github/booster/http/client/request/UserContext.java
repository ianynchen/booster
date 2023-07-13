package io.github.booster.http.client.request;

import io.vavr.Tuple;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User context to be included as part of web request
 */
@Getter
@Builder
public class UserContext {

    /**
     * Header name for device type
     */
    public static final String DEVICE_TYPE_HEADER = "X-Loblaw-Device-Type";

    /**
     * Header for accept language
     */
    public static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";

    /**
     * Header for business user agent
     */
    public static final String BUSINESS_AGENT_HEADER = "Business-User-Agent";

    /**
     * Header for tenant ID
     */
    public static final String TENANT_HEADER = "X-Loblaw-Tenant-ID";

    /**
     * Device type used by customer
     */
    private String deviceType;

    /**
     * Customer specified language preference
     */
    private String acceptLanguage;

    /**
     * Business user agent
     */
    private String businessAgent;

    /**
     * Tenant selected by customer.
     */
    private String tenant;

    /**
     * Creates a {@link Map} to be used to construct headers.
     * @return a {@link Map} with all non-blank fields. Empty if none of the fields exist.
     */
    public Map<String, String> createHeaders() {
        return List.of(
                Tuple.of(DEVICE_TYPE_HEADER, this.deviceType),
                Tuple.of(ACCEPT_LANGUAGE_HEADER, this.acceptLanguage),
                Tuple.of(BUSINESS_AGENT_HEADER, this.businessAgent),
                Tuple.of(TENANT_HEADER, this.tenant)
        ).stream()
                .filter(tuple -> StringUtils.isNotBlank(tuple._2()))
                .collect(Collectors.toMap(
                        tuple -> tuple._1(),
                        tuple -> tuple._2()
                ));
    }
}
