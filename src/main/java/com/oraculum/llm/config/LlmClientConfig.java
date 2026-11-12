package com.oraculum.llm.config;

import com.oraculum.llm.api.dto.LlmProviderType;
import okhttp3.Interceptor.Chain;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
public class LlmClientConfig {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);

    OpenAiChatModel buildChatModel(LlmProperties properties, LlmProviderType providerName) {
        var provider = properties.providers().get(providerName);
        if (provider == null) {
            throw new IllegalStateException("Missing credentials for provider: " + providerName);
        }

        var common = properties.common();
        Duration timeout = resolveDuration(provider.timeout(), common.timeout(), Duration.ZERO);
        Duration connectTimeout = resolveDuration(provider.connectTimeout(), common.connectTimeout(), DEFAULT_CONNECT_TIMEOUT);
        int maxRetries = firstNonNull(provider.maxRetries(), common.maxRetries(), 0);

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .apiKey(provider.apiKey())
                .baseUrl(provider.baseUrl())
                .temperature(common.temperature())
                .timeout(timeout)
                .maxRetries(maxRetries)
                .build();

        return OpenAiChatModel.builder()
                .options(options)
                .httpClientBuilderCustomizer(connectTimeoutCustomizer(connectTimeout))
                .build();
    }

    private Duration resolveDuration(Integer providerSeconds, Integer commonSeconds, Duration fallback) {
        Integer seconds = firstNonNull(providerSeconds, commonSeconds, null);
        if (seconds == null) {
            return fallback;
        }
        return seconds <= 0 ? Duration.ZERO : Duration.ofSeconds(seconds);
    }

    private <T> T firstNonNull(T first, T second, T fallback) {
        if (first != null) {
            return first;
        }
        return second != null ? second : fallback;
    }

    OpenAiHttpClientBuilderCustomizer connectTimeoutCustomizer(Duration connectTimeout) {
        int timeoutMillis = Math.toIntExact(connectTimeout.toMillis());
        return builder -> builder.interceptor(chain -> {
            Chain updated = chain.withConnectTimeout(timeoutMillis, TimeUnit.MILLISECONDS);
            return updated.proceed(updated.request());
        });
    }

    @Bean
    public Map<LlmProviderType, ChatClient> chatClients(LlmProperties properties) {
        Map<LlmProviderType, ChatClient> clients = new HashMap<>();

        properties.providers().forEach((providerName, _) -> {
            OpenAiChatModel chatModel = buildChatModel(properties, providerName);
            clients.put(providerName, ChatClient.builder(chatModel).build());
        });

        return clients;
    }
}
