package com.oraculum.company.api.dto;

import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.domain.TickerDocumentSubtype;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TickerDocumentDto {
    private final String id;
    private final String ticker;
    private final String market;
    private final TickerDocumentType documentType;
    private final TickerDocumentSubtype documentSubtype;
    private final LocalDate reportPeriod;
    private final String summary;
    private final Float sentimentScore;
}
