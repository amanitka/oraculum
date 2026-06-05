package com.oraculum.llm.config;

import com.oraculum.llm.api.dto.LlmTierType;
import com.oraculum.llm.domain.LlmProviderType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "oraculum.llm")
public record LlmProperties(Common common,
                            Retry retry,
                            Map<LlmProviderType, ProviderConfig> providers,
                            Map<LlmTierType, Map<LlmProviderType, String>> models) {

    public record Common(double temperature,
                         int maxCompletionTokens,
                         List<LlmProviderType> providerFallbackOrder,
                         Duration timeout) {
    }

    public record Retry(int maxRetries,
                        long initialBackoffMs) {
    }

    public record ProviderConfig(String baseUrl,
                                 String apiKey) {
    }
}