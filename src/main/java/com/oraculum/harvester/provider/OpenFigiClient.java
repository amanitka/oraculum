package com.oraculum.harvester.provider;

import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import com.oraculum.harvester.provider.dto.openfigi.OpenFigiMappingRequest;
import com.oraculum.harvester.provider.dto.openfigi.OpenFigiMappingResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
public class OpenFigiClient {

    private final RestClient restClient;

    public OpenFigiClient(@Qualifier("openFigiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retry(name = "openFigiApi")
    public List<OpenFigiMappingResponse> fetchTickersForCusips(List<String> cusips, String exchCode) {
        if (CollectionUtils.isEmpty(cusips)) {
            return List.of();
        }
        var items = cusips.stream()
                .map(cusip -> OpenFigiMappingRequest.ofCusip(cusip, exchCode))
                .toList();

        return restClient.post()
                .uri("/v3/mapping")
                .body(items)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}
