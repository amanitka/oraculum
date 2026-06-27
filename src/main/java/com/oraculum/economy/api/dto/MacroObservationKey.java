package com.oraculum.economy.api.dto;

import com.oraculum.economy.api.domain.MacroIndicator;

import java.time.LocalDate;

public record MacroObservationKey(MacroIndicator indicatorCode, LocalDate observationDate) {
}
