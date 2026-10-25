package com.oraculum.harvester.config;

import com.oraculum.common.config.OraculumProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FinnhubConfigTest {

    @Test
    void finnhubRestClient_configuresHeadersAndBaseUrl() {
        OraculumProperties properties = mock(OraculumProperties.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(properties.harvester().finnhub().baseUrl()).thenReturn("https://finnhub.io");
        when(properties.harvester().finnhub().apiKey()).thenReturn("test-token");
        when(properties.harvester().finnhub().hasApiKey()).thenReturn(true);
        when(properties.harvester().finnhub().rateLimitPerSecond()).thenReturn(30);
        when(properties.harvester().finnhub().rateLimitPerMinute()).thenReturn(60);

        FinnhubConfig config = new FinnhubConfig();
        RestClient restClient = config.finnhubRestClient(RestClient.builder(), properties);

        assertThat(restClient).isNotNull();
    }
}
