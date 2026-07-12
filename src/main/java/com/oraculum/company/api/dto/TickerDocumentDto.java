package com.oraculum.company.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TickerDocumentDto {
    private final String id;
    private final String ticker;
    private final String market;
    private final String documentType;
    private final String documentSubtype;
    private final LocalDate reportPeriod;
    private final String summary;
    private final Float sentimentScore;
}
