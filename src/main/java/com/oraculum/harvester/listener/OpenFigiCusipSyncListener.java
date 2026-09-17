package com.oraculum.harvester.listener;

import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.company.api.CompanySecApi;
import com.oraculum.company.api.dto.TickerKeyDto;
import com.oraculum.harvester.event.FetchCompanyCusipsRequestEvent;
import com.oraculum.harvester.service.OpenFigiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenFigiCusipSyncListener {

    private final OpenFigiService openFigiService;
    private final CompanySecApi companySecApi;
    private final CompanyMetadataApi companyMetadataApi;

    @EventListener
    @SuppressWarnings("unused")
    public void onFetchCompanyCusips(FetchCompanyCusipsRequestEvent event) {
        log.info("Starting OpenFIGI CUSIP synchronization...");
        List<String> distinctCusips = companySecApi.getDistinctHoldingCusips();
        log.info("Found {} distinct CUSIPs in sec holdings to resolve via OpenFIGI.", distinctCusips.size());
        Map<TickerKeyDto, String> tickerToCusipMap = openFigiService.resolveCusips(distinctCusips);
        companyMetadataApi.updateCompanyCusips(tickerToCusipMap);
        log.info("OpenFIGI CUSIP synchronization completed successfully with {} resolved mappings.", tickerToCusipMap.size());
    }
}
