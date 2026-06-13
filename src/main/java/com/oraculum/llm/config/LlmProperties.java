package com.oraculum.llm.config;

import com.oraculum.llm.api.dto.LlmTierType;
import com.oraculum.llm.domain.LlmProviderType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "oraculum.llm")
public record LlmProperties(Common common,
                            Map<LlmProviderType, ProviderConfig> providers,
                            Map<LlmTierType, Map<LlmProviderType, String>> models) {

    public record Common(double temperature,
                         int maxCompletionTokens,
                         List<LlmProviderType> providerFallbackOrder,
                         Integer timeout,
                         Integer maxRetries,
                         Long retryDelayMs) {
    }

    public record ProviderConfig(String baseUrl,
                                 String apiKey) {
    }
}