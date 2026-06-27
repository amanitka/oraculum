package com.oraculum.harvester.service;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.economy.api.EconomyDataApi;
import com.oraculum.economy.api.domain.MacroIndicator;
import com.oraculum.economy.api.dto.MacroObservationDto;
import com.oraculum.harvester.event.FetchMacroeconomicRequestEvent;
import com.oraculum.harvester.provider.FredClient;
import com.oraculum.harvester.provider.dto.FredResponse;
import com.oraculum.util.NumericUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FredService {
    private final OraculumProperties oraculumProperties;
    private final EconomyDataApi economyDataApi;
    private final FredClient fredClient;


    public List<MacroObservationDto> getMacroObservationDtoFromResponse(MacroIndicator indicator, FredResponse response) {
        if (response == null || response.observations() == null) {
            return List.of();
        }
        return response.observations().stream()
                .map(observation -> new MacroObservationDto(indicator, observation.date(), NumericUtil.stringToDouble(observation.value())))
                .collect(Collectors.toList());
    }

    @EventListener(FetchMacroeconomicRequestEvent.class)
    @Transactional
    public void refreshMacroeconomic() {
        String dateFromString = LocalDate.now().minusYears(oraculumProperties.harvester().fred().historyLimitYears()).toString();
        List<MacroObservationDto> macroObservationDtoList = new ArrayList<>(5000);
        for (MacroIndicator indicator : MacroIndicator.values()) {
            try {
                log.info("Fetching macroeconomic data from FRED. SeriesId - {}, DateFrom - {}", indicator, dateFromString);
                FredResponse response = fredClient.fetchMacroeconomicData(indicator.name(), dateFromString);
                macroObservationDtoList.addAll(getMacroObservationDtoFromResponse(indicator, response));
            } catch (Exception e) {
                log.error("Error fetching macroeconomic data from FRED. SeriesId - {}, DateFrom - {}", indicator, dateFromString, e);
            }
        }
        log.info("Fetched macroeconomic data from FRED. Total observations - {}", macroObservationDtoList.size());
        log.info("Saving macroeconomic observations in database");
        economyDataApi.createOrUpdateObservations(macroObservationDtoList);
    }
}
