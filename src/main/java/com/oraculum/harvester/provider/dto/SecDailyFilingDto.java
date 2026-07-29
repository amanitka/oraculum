package com.oraculum.harvester.provider.dto;

import java.time.LocalDate;
import com.oraculum.company.api.domain.TickerDocumentType;

public record SecDailyFilingDto(
    String cik,
    String companyName,
    TickerDocumentType formType,
    LocalDate dateFiled,
    String filename
) {}
