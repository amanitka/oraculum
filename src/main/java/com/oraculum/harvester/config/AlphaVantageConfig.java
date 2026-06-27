package com.oraculum.harvester.config;

import com.oraculum.common.config.OraculumProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AlphaVantageConfig {

    @Bean
    public RestClient alphaVantageRestClient(RestClient.Builder builder, OraculumProperties properties) {
        String apiKey = properties.harvester().alphaVantage().apiKey();
        return builder
                .baseUrl(properties.harvester().alphaVantage().baseUrl())
                .requestInterceptor(new ApiKeyQueryParamInterceptor("apiKey", apiKey))
                .build();
    }
}
