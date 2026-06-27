package com.oraculum.economy.api;

import com.oraculum.economy.api.domain.MacroIndicator;
import com.oraculum.economy.api.dto.MacroObservationDto;

import java.util.List;

public interface EconomyDataApi {
    void createOrUpdateObservations(List<MacroObservationDto> observations);

    List<MacroObservationDto> getObservations(MacroIndicator indicator);
}
