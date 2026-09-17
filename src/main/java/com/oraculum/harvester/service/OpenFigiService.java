package com.oraculum.harvester.service;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenFigiService {

    public static final String US = "US";
    private static final int BATCH_SIZE = 100;
    private final OpenFigiClient openFigiClient;

    public Map<TickerKeyDto, String> resolveCusips(List<String> cusips) {
        Map<TickerKeyDto, String> resultMap = new HashMap<>();
        CollectionUtil.forEachPartition(cusips, BATCH_SIZE, chunk -> processChunk(chunk, resultMap));

        log.info("Resolved {} / {} CUSIPs via OpenFIGI.", resultMap.size(), cusips != null ? cusips.size() : 0);
        return resultMap;
    }

    private void processChunk(List<String> chunk, Map<TickerKeyDto, String> resultMap) {
        List<OpenFigiMappingResponse> responses = openFigiClient.fetchTickersForCusips(chunk, US);
        if (CollectionUtils.isEmpty(responses)) {
            return;
        }

        for (int i = 0; i < Math.min(chunk.size(), responses.size()); i++) {
            OpenFigiMappingResponse res = responses.get(i);
            if (res.hasData()) {
                String ticker = res.data().getFirst().ticker();
                if (ticker != null && !ticker.isBlank()) {
                    resultMap.put(new TickerKeyDto(ticker.trim().toUpperCase(), US), chunk.get(i));
                }
            }
        }
    }
}
