package com.oraculum.company.api;

import com.oraculum.company.api.dto.InsiderTransactionSummaryDto;
import com.oraculum.company.api.dto.InsiderTransactionTickerDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CompanyInsiderTransactionApi {
    Optional<LocalDateTime> getInsiderTransactionsLastFilingDate();
    Optional<InsiderTransactionSummaryDto> getInsiderTransactionSummaryByTicker(String ticker);
    List<InsiderTransactionTickerDto> getInsiderTransactionsByTicker(String ticker, LocalDate after);
}
