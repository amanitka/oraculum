package com.oraculum.llm.config;

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
    public Map<String, ChatClient> primaryClients(LlmProperties properties, ChatClient.Builder builder) {
        return buildClients(properties, true, builder);
    }

    @Bean
    public Map<String, ChatClient> secondaryClients(LlmProperties properties, ChatClient.Builder builder) {
        return buildClients(properties, false, builder);
    }

    private Map<String, ChatClient> buildClients(LlmProperties properties, boolean isPrimary,
                                                 ChatClient.Builder builder) {
        Map<String, ChatClient> clients = new HashMap<>();

        properties.tiers().forEach((tierName, tierConfig) -> {
            LlmProperties.ModelReference modelRef = isPrimary ? tierConfig.primary() : tierConfig.secondary();
            LlmProperties.ProviderConfig provider = properties.providers().get(modelRef.provider());

            if (provider == null) {
                throw new IllegalStateException("Missing credentials for provider token: " + modelRef.provider());
            }

            var api = new OpenAiApi(provider.baseUrl(), provider.apiKey());
            var options =
                    OpenAiChatOptions.builder().model(modelRef.model()).temperature(properties.common().temperature()).build();

            var chatModel = new OpenAiChatModel(api, options);
            clients.put(tierName, builder.clone().chatModel(chatModel).build());
        });

        return clients;
    }
}
