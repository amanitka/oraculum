package com.oraculum.llm.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.oraculum.llm.property.LlmProperties;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class LlmClientConfig {

    @Bean
    public Map<String, ChatClient> primaryClients(LlmProperties properties) {
        return buildClients(properties, true);
    }

    @Bean
    public Map<String, ChatClient> secondaryClients(LlmProperties properties) {
        return buildClients(properties, false);
    }

    private OpenAiChatModel buildChatModel(LlmProperties properties, LlmProperties.ModelReference modelRef) {
        var provider = properties.providers().get(modelRef.provider());
        if (provider == null) {
            throw new IllegalStateException("Missing credentials for provider: " + modelRef.provider());
        }
        // OpenAI SDK client (works for OpenAI + Gemini + others)
        OpenAIClient openAiClient =
                OpenAIOkHttpClient.builder().apiKey(provider.apiKey()).baseUrl(provider.baseUrl()).build();
        // Model options
        OpenAiChatOptions options =
                OpenAiChatOptions.builder().model(modelRef.model()).temperature(properties.common().temperature()).build();
        // Return chat model
        return OpenAiChatModel.builder().openAiClient(openAiClient).options(options).build();
    }

    private Map<String, ChatClient> buildClients(LlmProperties properties, boolean isPrimary) {
        Map<String, ChatClient> clients = new HashMap<>();
        properties.tiers().forEach((tierName, tierConfig) -> {
            var modelRef = isPrimary ? tierConfig.primary() : tierConfig.secondary();
            clients.put(tierName, ChatClient.builder(buildChatModel(properties, modelRef)).build());
        });

        return clients;
    }
}
