package com.oraculum.llm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "oraculum.llm")
@Getter
@Setter
public class LlmProperties {

    private final List<Deployment> deployments = new ArrayList<>();
    private final RouterSettings routerSettings = new RouterSettings();

    @Getter
    @Setter
    public static class Deployment {
        private String alias;
        private String model;
        private String apiKey;
        private String apiBase;
        private int order;
    }

    @Getter
    @Setter
    public static class RouterSettings {
        private double temperature;
        private int numRetries;
        private int workflowTokenBudget;
        private int maxTokens;
    }
}
