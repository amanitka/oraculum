package com.oraculum.llm.property;

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
                         List<LlmProviderType> providerFallbackOrder) {
    }

    public record ProviderConfig(String baseUrl,
                                 String apiKey) {
    }
}