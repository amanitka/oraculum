package com.oraculum.analyst.config;

import com.oraculum.analyst.domain.PromptType;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

@Component
public class PromptRegistry {

    private final Map<PromptType, String> prompts = new EnumMap<>(PromptType.class);

    @PostConstruct
    public void init() {
        for (PromptType type : PromptType.values()) {
            try (InputStream in = getClass().getResourceAsStream(type.getPath())) {
                if (in == null) {
                    throw new IOException("Prompt file not found: " + type.getPath());
                }
                prompts.put(type, new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException("Failed to load prompt: " + type.name(), e);
            }
        }
    }

    public String getPrompt(PromptType type) {
        return prompts.get(type);
    }
}
