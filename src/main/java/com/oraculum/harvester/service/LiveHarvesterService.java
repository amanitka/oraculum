package com.oraculum.harvester.service;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.harvester.api.HarvesterLiveApi;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import com.oraculum.harvester.domain.ProviderType;
import com.oraculum.harvester.provider.AlphaVantageClient;
import com.oraculum.harvester.provider.dto.AlphaVantageEarningsEstimatesResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class LiveHarvesterService implements HarvesterLiveApi {

    private final AlphaVantageClient alphaVantageClient;
    private final ApiUsageTrackerService apiUsageTrackerService;
    private final int dailyLimit;
    private final int reservedCalls;

    public LiveHarvesterService(AlphaVantageClient alphaVantageClient,
                                ApiUsageTrackerService apiUsageTrackerService,
                                OraculumProperties properties) {
        this.alphaVantageClient = alphaVantageClient;
        this.apiUsageTrackerService = apiUsageTrackerService;
        this.dailyLimit = properties.harvester().alphaVantage().dailyLimit();
        this.reservedCalls = properties.harvester().alphaVantage().reservedCalls();
    }

    @Override
    public Optional<List<EarningsEstimateDto>> fetchEarningsEstimates(String ticker) {
        int availableLimit = Math.max(0, dailyLimit - reservedCalls);
        if (!apiUsageTrackerService.canMakeCall(ProviderType.ALPHA_VANTAGE, availableLimit)) {
            log.warn("Alpha Vantage available limit (excluding reserved calls) reached. Skipping earnings estimates for {}.", ticker);
            return Optional.empty();
        }

        try {
            AlphaVantageEarningsEstimatesResponse response = alphaVantageClient.fetchEarningsEstimates(ticker);
            apiUsageTrackerService.recordCall(ProviderType.ALPHA_VANTAGE);

            if (response == null || response.estimates() == null) {
                return Optional.empty();
            }

            return Optional.of(response.estimates());
        } catch (Exception e) {
            log.error("Failed to fetch earnings estimates for {}", ticker, e);
            return Optional.empty();
        }
    }
}
