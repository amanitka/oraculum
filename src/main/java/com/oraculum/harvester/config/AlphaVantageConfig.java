package com.oraculum.harvester.config;

import com.oraculum.common.config.OraculumProperties;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AlphaVantageConfig {

    @Bean
    public RestClient alphaVantageRestClient(RestClient.Builder builder, OraculumProperties properties) {
        String apiKey = properties.harvester().alphaVantage().apiKey();
        builder.baseUrl(properties.harvester().alphaVantage().baseUrl())
                .requestInterceptor(new ApiKeyQueryParamInterceptor("apikey", apiKey));

        if (LoggerFactory.getLogger(RestClientLoggingInterceptor.class).isDebugEnabled()) {
            builder.requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
                    .requestInterceptor(new RestClientLoggingInterceptor());
        }

        return builder.build();
    }
}
