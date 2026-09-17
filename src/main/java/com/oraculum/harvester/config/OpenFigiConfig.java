package com.oraculum.harvester.config;

import com.oraculum.common.config.OraculumProperties;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class OpenFigiConfig {

    private RateLimiter getOpenFigiRateLimiter(int rateLimitPerSecond) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(rateLimitPerSecond)
                .timeoutDuration(Duration.ofMinutes(1))
                .build();

        return RateLimiter.of("openFigi", config);
    }

    @Bean("openFigiRestClient")
    public RestClient openFigiRestClient(RestClient.Builder builder, OraculumProperties properties) {
        var openFigiConfig = properties.harvester().openFigi();
        RateLimiter rateLimiter = getOpenFigiRateLimiter(openFigiConfig.rateLimitPerSecond());

        builder.baseUrl(openFigiConfig.baseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-OPENFIGI-KEY", openFigiConfig.apiKey())
                .requestInterceptor((request, body, execution) -> {
                    RateLimiter.waitForPermission(rateLimiter);
                    return execution.execute(request, body);
                });

        if (LoggerFactory.getLogger(RestClientLoggingInterceptor.class).isDebugEnabled()) {
            builder.requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
                    .requestInterceptor(new RestClientLoggingInterceptor());
        }

        return builder.build();
    }
}
