package com.oraculum.economy.api.dto;

import com.oraculum.economy.api.domain.MacroIndicator;

import java.time.LocalDate;

public record MacroSummaryDto(
        MacroIndicator indicator,
        String indicatorTitle,
        LocalDate latestDate,
        Double latestValue,
        Double value1yAgo,
        Double yoyChangePct,
        Double min1y,
        Double max1y,
        Double avg1y,
        Double diffFrom1yAvg
) {
}
