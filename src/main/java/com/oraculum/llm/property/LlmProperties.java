package com.oraculum.llm.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "oraculum.llm")
public record LlmProperties(CommonProperties common, Map<String, ProviderConfig> providers,
                            Map<String, TierConfig> tiers) {
    public record CommonProperties(Double temperature, Integer maxRetries, Long initialBackoffMs) {
    }

    public record ProviderConfig(String baseUrl, String apiKey) {
    }

    public record TierConfig(ModelReference primary, ModelReference secondary) {
    }

    public record ModelReference(String provider, String model) {
    }
}