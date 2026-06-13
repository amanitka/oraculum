package com.oraculum.llm.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.oraculum.llm.domain.LlmProviderType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class LlmClientConfig {

    private OpenAiChatModel buildChatModel(LlmProperties properties, LlmProviderType providerName) {
        var provider = properties.providers().get(providerName);
        if (provider == null) {
            throw new IllegalStateException("Missing credentials for provider: " + providerName);
        }
        Duration timeout = properties.common().timeout() != null
                ? Duration.ofSeconds(properties.common().timeout())
                : Duration.ofSeconds(60);
        OpenAIClient openAiClient = OpenAIOkHttpClient.builder()
                .apiKey(provider.apiKey())
                .baseUrl(provider.baseUrl())
                .timeout(timeout)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder().temperature(properties.common().temperature()).timeout(timeout).build();

        return OpenAiChatModel.builder()
                .openAiClient(openAiClient)
                .options(options)
                .build();
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
