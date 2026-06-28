package com.oraculum.economy.api;

import com.oraculum.economy.api.dto.MacroObservationDto;
import com.oraculum.economy.api.dto.MacroSummaryDto;

import java.util.List;

public interface EconomyDataApi {
    void createOrUpdateObservations(List<MacroObservationDto> observations);

    List<MacroSummaryDto> getMacroeconomicSummary();

    List<MacroObservationDto> getHistoricalData(com.oraculum.economy.api.domain.MacroIndicator indicator);
}
