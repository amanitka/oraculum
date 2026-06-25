package com.oraculum.company.service.impl;

import com.oraculum.company.api.CompanyInsiderTransactionApi;
import com.oraculum.company.api.dto.InsiderTransactionSummaryDto;
import com.oraculum.company.api.dto.InsiderTransactionTickerDto;
import com.oraculum.company.repository.InsiderTransactionSummaryRepository;
import com.oraculum.company.repository.InsiderTransactionTickerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanyInsiderTransactionServiceImpl implements CompanyInsiderTransactionApi {

    private final InsiderTransactionTickerRepository insiderTransactionTickerRepository;
    private final InsiderTransactionSummaryRepository insiderTransactionSummaryRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDateTime> getInsiderTransactionsLastFilingDate() {
        return insiderTransactionTickerRepository.findMaxFilingDate();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InsiderTransactionSummaryDto> getInsiderTransactionSummaryByTicker(String ticker) {
        return insiderTransactionSummaryRepository.findById(ticker)
                .map(InsiderTransactionSummaryDto::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InsiderTransactionTickerDto> getInsiderTransactionsByTicker(String ticker, LocalDate after) {
        return insiderTransactionTickerRepository.findByTickerAndTradeDateAfterOrderByFilingDateDesc(ticker, after).stream()
                .map(InsiderTransactionTickerDto::fromEntity)
                .collect(Collectors.toList());
    }
}
