package com.oraculum.harvester.config;

import com.oraculum.common.config.OraculumProperties;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class SecEdgarConfig {

    private RateLimiter getSecRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(10) // SEC limit: 10 requests per second
                .timeoutDuration(Duration.ofMinutes(1)) // Wait up to 1 minute if queued
                .build();

        return RateLimiter.of("secEdgar", config);
    }

    @Bean("secEdgarRestClient")
    public RestClient secEdgarRestClient(RestClient.Builder builder, OraculumProperties properties) {
        String baseUrl = properties.harvester().secEdgar().baseUrl();
        String userAgent = properties.harvester().secEdgar().userAgent();

        builder.baseUrl(baseUrl)
                .defaultHeader("User-Agent", userAgent)
                .requestInterceptor((request, body, execution) -> {
                    RateLimiter.waitForPermission(getSecRateLimiter());
                    return execution.execute(request, body);
                });

        if (LoggerFactory.getLogger(RestClientLoggingInterceptor.class).isDebugEnabled()) {
            builder.requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
                    .requestInterceptor(new RestClientLoggingInterceptor());
        }

        return builder.build();
    }
}
