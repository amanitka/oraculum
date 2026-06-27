package com.oraculum.harvester.config;

import com.oraculum.common.config.OraculumProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;


@Configuration
public class FredConfig {

    @Bean
    public RestClient fredRestClient(RestClient.Builder builder, OraculumProperties properties) {
        String apiKey = properties.harvester().fred().apiKey();
        return builder
                .baseUrl(properties.harvester().fred().baseUrl())
                .requestInterceptor(new ApiKeyQueryParamInterceptor("api_key", apiKey))
                .build();
    }

}
