package com.oraculum.company.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TickerDocumentRawDto {
    private final String id;
    private final String ticker;
    private final String market;
    private final String documentType;
    private final String documentSubtype;
    private final LocalDate reportPeriod;
    private final LocalDate filingDate;
    private final String content;
}
