package com.oraculum.company.api.dto;

import com.oraculum.company.api.domain.TickerDocumentType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TickerDocumentSyncStatusDto {
    private final String ticker;
    private final String market;
    private final TickerDocumentType documentType;
    private final LocalDate lastProcessedFileDate;
}
