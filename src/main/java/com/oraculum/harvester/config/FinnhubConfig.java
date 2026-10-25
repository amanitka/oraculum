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
public class FinnhubConfig {

    private RateLimiter createRateLimiter(String name, Duration period, int limit) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(period)
                .limitForPeriod(limit)
                .timeoutDuration(Duration.ofMinutes(1))
                .build();
        return RateLimiter.of(name, config);
    }

    private void addRateLimiters(RestClient.Builder builder, OraculumProperties.Harvester.Finnhub finnhubConfig) {
        RateLimiter secondLimiter = createRateLimiter("finnhubSecond", Duration.ofSeconds(1), finnhubConfig.rateLimitPerSecond());
        RateLimiter minuteLimiter = createRateLimiter("finnhubMinute", Duration.ofMinutes(1), finnhubConfig.rateLimitPerMinute());

        builder.requestInterceptor((request, body, execution) -> {
            RateLimiter.waitForPermission(secondLimiter);
            RateLimiter.waitForPermission(minuteLimiter);
            return execution.execute(request, body);
        });
    }

    @Bean("finnhubRestClient")
    public RestClient finnhubRestClient(RestClient.Builder builder, OraculumProperties properties) {
        var finnhubConfig = properties.harvester().finnhub();
        builder.baseUrl(finnhubConfig.baseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE);

        if (finnhubConfig.hasApiKey()) {
            builder.defaultHeader("X-Finnhub-Token", finnhubConfig.apiKey().trim());
        }

        addRateLimiters(builder, finnhubConfig);

        if (LoggerFactory.getLogger(RestClientLoggingInterceptor.class).isDebugEnabled()) {
            builder.requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
                    .requestInterceptor(new RestClientLoggingInterceptor());
        }

        return builder.build();
    }
}
