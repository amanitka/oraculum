package com.oraculum.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "oraculum.oauth2")
public record OAuth2Properties(
        Keycloak keycloak,
        Google google
) {
    public record Keycloak(
            String clientId,
            String clientSecret,
            String issuerUri,
            List<String> scopes
    ) {
        public boolean isConfigured() {
            return isNotBlank(clientId) && isNotBlank(clientSecret) && isNotBlank(issuerUri);
        }
    }

    public record Google(
            String clientId,
            String clientSecret,
            List<String> scopes
    ) {
        public boolean isConfigured() {
            return isNotBlank(clientId) && isNotBlank(clientSecret);
        }
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
