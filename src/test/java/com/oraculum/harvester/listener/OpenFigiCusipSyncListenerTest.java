package com.oraculum.harvester.listener;

import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.company.api.CompanySecApi;
import com.oraculum.company.api.dto.TickerKeyDto;
import com.oraculum.harvester.event.FetchCompanyCusipsRequestEvent;
import com.oraculum.harvester.service.OpenFigiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenFigiCusipSyncListenerTest {

    @Mock
    private OpenFigiService openFigiService;

    @Mock
    private CompanySecApi companySecApi;

    @Mock
    private CompanyMetadataApi companyMetadataApi;

    private OpenFigiCusipSyncListener listener;

    @BeforeEach
    void setUp() {
        listener = new OpenFigiCusipSyncListener(openFigiService, companySecApi, companyMetadataApi);
    }

    @Test
    void onFetchCompanyCusips_resolvesAndUpdatesCompanyCusips() {
        List<String> cusips = List.of("007903107", "037833100");
        Map<TickerKeyDto, String> resolved = Map.of(
                new TickerKeyDto("AMD", "US"), "007903107",
                new TickerKeyDto("AAPL", "US"), "037833100"
        );

        when(companySecApi.getDistinctHoldingCusips()).thenReturn(cusips);
        when(openFigiService.resolveCusips(cusips)).thenReturn(resolved);

        listener.onFetchCompanyCusips(new FetchCompanyCusipsRequestEvent());

        verify(companyMetadataApi).updateCompanyCusips(resolved);
    }
}
