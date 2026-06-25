package com.oraculum.company.api;

import com.oraculum.company.api.dto.SharePriceDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CompanySharePriceApi {
    List<SharePriceDto> getSharePricesByCompanyId(int companyId, LocalDate after);
    Optional<LocalDate> getSharePricesLastTradeDate();
    List<SharePriceSignalDto> getDailySharePriceSignalsByCompanyId(int companyId, LocalDate after);
    List<SharePriceSignalDto> getMonthlySharePriceSignalsByCompanyId(int companyId, LocalDate after);
}
