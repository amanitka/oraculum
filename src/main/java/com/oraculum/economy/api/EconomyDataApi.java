package com.oraculum.economy.api;

import com.oraculum.economy.api.dto.MacroObservationDto;
import com.oraculum.economy.api.dto.MacroSummaryDto;

import java.util.List;

public interface EconomyDataApi {
    void createOrUpdateObservations(List<MacroObservationDto> observations);

    List<MacroSummaryDto> getMacroeconomicSummary();
}
