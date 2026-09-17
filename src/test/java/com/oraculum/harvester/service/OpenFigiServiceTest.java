package com.oraculum.harvester.service;

import com.oraculum.company.api.dto.TickerKeyDto;
import com.oraculum.harvester.provider.OpenFigiClient;
import com.oraculum.harvester.provider.dto.openfigi.OpenFigiMappingResponse;
import com.oraculum.harvester.provider.dto.openfigi.OpenFigiResultItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenFigiServiceTest {

    @Mock
    private OpenFigiClient openFigiClient;

    private OpenFigiService openFigiService;

    @BeforeEach
    void setUp() {
        openFigiService = new OpenFigiService(openFigiClient);
    }

    @Test
    void resolveCusips_whenInputEmpty_returnsEmptyMap() {
        Map<TickerKeyDto, String> result = openFigiService.resolveCusips(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void resolveCusips_resolvesValidCusipsToTickerKeyMap() {
        String cusipAmd = "007903107";
        String cusipAapl = "037833100";

        OpenFigiResultItem itemAmd = new OpenFigiResultItem(
                "BBG000BBQCY0", "ADVANCED MICRO DEVICES", "AMD", "US", "BBG000BBQCY0", "Common Stock", "Equity", "BBG001S5NN36", "AMD"
        );
        OpenFigiResultItem itemAapl = new OpenFigiResultItem(
                "BBG000B9XRY4", "APPLE INC", "AAPL", "US", "BBG000B9XRY4", "Common Stock", "Equity", "BBG001S5N8V8", "AAPL"
        );

        when(openFigiClient.fetchTickersForCusips(anyList(), anyString())).thenReturn(List.of(
                new OpenFigiMappingResponse(List.of(itemAmd), null, null),
                new OpenFigiMappingResponse(List.of(itemAapl), null, null)
        ));

        Map<TickerKeyDto, String> result = openFigiService.resolveCusips(List.of(cusipAmd, cusipAapl));

        assertThat(result).hasSize(2);
        assertThat(result.get(new TickerKeyDto("AMD", "US"))).isEqualTo(cusipAmd);
        assertThat(result.get(new TickerKeyDto("AAPL", "US"))).isEqualTo(cusipAapl);
        verify(openFigiClient).fetchTickersForCusips(List.of(cusipAmd, cusipAapl), "US");
    }

    @Test
    void resolveCusips_whenNoDataInResponse_skipsCusip() {
        String cusip = "999999999";
        when(openFigiClient.fetchTickersForCusips(anyList(), anyString())).thenReturn(List.of(
                new OpenFigiMappingResponse(null, "No identifier found.", null)
        ));

        Map<TickerKeyDto, String> result = openFigiService.resolveCusips(List.of(cusip));

        assertThat(result).isEmpty();
    }

    @Test
    void resolveCusips_whenMoreThanBatchSize_batchesRequests() {
        List<String> manyCusips = java.util.stream.IntStream.range(0, 150)
                .mapToObj(i -> String.format("%09d", i))
                .toList();

        when(openFigiClient.fetchTickersForCusips(anyList(), anyString())).thenReturn(List.of());

        openFigiService.resolveCusips(manyCusips);

        verify(openFigiClient).fetchTickersForCusips(manyCusips.subList(0, 100), "US");
        verify(openFigiClient).fetchTickersForCusips(manyCusips.subList(100, 150), "US");
    }
}
