package com.oraculum.economy.api.dto;

import com.oraculum.economy.api.domain.MacroIndicator;

import java.time.LocalDate;

public record MacroObservationDto(
        MacroIndicator indicatorCode,
        LocalDate observationDate,
        Double value
) {
    public MacroObservationKey getKey() {
        return new MacroObservationKey(indicatorCode, observationDate);
    }
}
