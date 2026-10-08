package com.oraculum.harvester.service;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.common.constant.MarketConstants;
import com.oraculum.company.api.dto.TickerKeyDto;
import com.oraculum.harvester.provider.OpenFigiClient;
import com.oraculum.harvester.provider.dto.openfigi.OpenFigiMappingResponse;
import com.oraculum.util.CollectionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenFigiService {

    private final OpenFigiClient openFigiClient;
    private final OraculumProperties properties;

    public Map<TickerKeyDto, String> resolveCusips(List<String> cusips) {
        if (CollectionUtils.isEmpty(cusips)) {
            return Map.of();
        }
        Map<TickerKeyDto, String> resultMap = new HashMap<>();
        int batchSize = getBatchSize();
        int totalBatches = (cusips.size() + batchSize - 1) / batchSize;
        AtomicInteger batchCounter = new AtomicInteger();

        CollectionUtil.forEachPartition(cusips, batchSize, chunk -> {
            processChunk(chunk, resultMap);
            int current = batchCounter.incrementAndGet();
            if (current % 25 == 0 || current == totalBatches) {
                log.info("OpenFIGI progress: {}/{} batches ({}%). Matched {} tickers so far.", current, totalBatches, (current * 100) / totalBatches, resultMap.size());
            }
        });

        log.info("Resolved {} / {} CUSIPs via OpenFIGI.", resultMap.size(), cusips.size());
        return resultMap;
    }

    private int getBatchSize() {
        if (properties != null && properties.harvester() != null && properties.harvester().openFigi() != null) {
            return properties.harvester().openFigi().getBatchSize();
        }
        return 100;
    }

    private void processChunk(List<String> chunk, Map<TickerKeyDto, String> resultMap) {
        List<OpenFigiMappingResponse> responses = openFigiClient.fetchTickersForCusips(chunk, MarketConstants.US);
        if (CollectionUtils.isEmpty(responses)) {
            return;
        }

        for (int i = 0; i < Math.min(chunk.size(), responses.size()); i++) {
            OpenFigiMappingResponse res = responses.get(i);
            if (res.hasData()) {
                String ticker = res.data().getFirst().ticker();
                if (ticker != null && !ticker.isBlank()) {
                    resultMap.put(new TickerKeyDto(ticker.trim().toUpperCase(), MarketConstants.US), chunk.get(i));
                }
            }
        }
    }
}
