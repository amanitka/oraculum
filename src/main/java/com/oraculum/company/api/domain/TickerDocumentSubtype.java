package com.oraculum.company.api.domain;

import lombok.Getter;

import java.time.Period;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

@Getter
public enum TickerDocumentSubtype {
    SEC_RF("SEC_RF", 1, Period.ofYears(2)),
    SEC_MD("SEC_MD", 4, Period.ofYears(2)),
    SEC_EX99_1("SEC_EX99_1", 4, Period.ofYears(1)),
    TRANSCRIPT("TRANSCRIPT", 4, Period.ofYears(1));

    private static final int MAX_PROCESSING_LIMIT;
    private static final Period MAX_PROCESSING_PERIOD;

    static {
        // Aggregate maxFileLimit (maximum integer value)
        MAX_PROCESSING_LIMIT = Arrays.stream(values())
                .map(TickerDocumentSubtype::getProcessingLimit)
                .max(Integer::compareTo)
                .orElse(0);

        // Aggregate maxHistoryLimit (maximum Period based on total estimated months/years)
        // Note: Period comparison can be tricky, so standardizing to total months is robust.
        MAX_PROCESSING_PERIOD = Arrays.stream(values())
                .map(TickerDocumentSubtype::getProcessingPeriod)
                .filter(Objects::nonNull)
                .max(Comparator.comparing(Period::toTotalMonths))
                .orElse(Period.ZERO);
    }

    private final String code;
    private final int processingLimit;
    private final Period processingPeriod;

    TickerDocumentSubtype(String code, int processingLimit, Period processingPeriod) {
        this.code = code;
        this.processingLimit = processingLimit;
        this.processingPeriod = processingPeriod;
    }

    public static int getMaxProcessingLimit() {
        return MAX_PROCESSING_LIMIT;
    }

    public static Period getMaxProcessingPeriod() {
        return Objects.requireNonNullElse(MAX_PROCESSING_PERIOD, Period.ofYears(2));
    }
}
