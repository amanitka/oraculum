package com.oraculum.llm.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "oraculum.llm")
public record LlmProperties(Common common, Map<String, ProviderConfig> providers,
                            Map<String, Map<String, String>> models) {

    public record Common(double temperature, List<String> providerFallbackOrder) {
    }

    public record ProviderConfig(String baseUrl, String apiKey) {
    }
}