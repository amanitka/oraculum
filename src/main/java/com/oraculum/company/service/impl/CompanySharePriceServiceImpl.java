package com.oraculum.company.service.impl;

import com.oraculum.company.api.CompanySharePriceApi;
import com.oraculum.company.api.dto.SharePriceDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import com.oraculum.company.repository.SharePriceRepository;
import com.oraculum.company.repository.SharePriceSignalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompanySharePriceServiceImpl implements CompanySharePriceApi {

    private final SharePriceRepository sharePriceRepository;
    private final SharePriceSignalRepository sharePriceSignalRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SharePriceDto> getSharePricesByCompanyId(int companyId, LocalDate after) {
        return sharePriceRepository.findByCompanyIdAndTradeDateAfter(companyId, after)
                .stream()
                .map(SharePriceDto::fromEntity)
                .sorted(Comparator.comparing(SharePriceDto::tradeDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDate> getSharePricesLastTradeDate() {
        return sharePriceRepository.findMaxTradeDate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SharePriceSignalDto> getDailySharePriceSignalsByCompanyId(int companyId, LocalDate after) {
        return sharePriceSignalRepository.findByCompanyIdAndTradeDateAfter(companyId, after)
                .stream()
                .map(SharePriceSignalDto::fromEntity)
                .sorted(Comparator.comparing(SharePriceSignalDto::tradeDate).reversed())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SharePriceSignalDto> getMonthlySharePriceSignalsByCompanyId(int companyId, LocalDate after) {
        return sharePriceSignalRepository.findByCompanyIdAndTradeDateAfterAndFlagLastDayOfMonth(companyId, after, "Y")
                .stream()
                .map(SharePriceSignalDto::fromEntity)
                .sorted(Comparator.comparing(SharePriceSignalDto::tradeDate).reversed())
                .collect(Collectors.toList());
    }
}
