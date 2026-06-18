package com.oraculum.harvester.config;

import com.oraculum.common.config.OraculumProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Configuration
public class HarvesterConfig {

    @Bean
    public RestClient alphaVantageRestClient(RestClient.Builder builder, OraculumProperties properties) {
        String apiKey = properties.harvester().alphaVantage().apiKey();
        return builder
                .baseUrl(properties.harvester().alphaVantage().baseUrl())
                .requestInterceptor((request, body, execution) -> {
                    URI newUri = UriComponentsBuilder.fromUri(request.getURI())
                            .queryParam("apikey", apiKey)
                            .build()
                            .toUri();
                    HttpRequest modifiedRequest = new HttpRequestWrapper(request) {
                        @Override
                        public @NonNull URI getURI() {
                            return newUri;
                        }
                    };
                    return execution.execute(modifiedRequest, body);
                })
                .build();
    }
}
