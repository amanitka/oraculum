package com.oraculum.company.api;

import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.IndustryDto;
import com.oraculum.company.api.dto.MarketDto;

import java.util.List;
import java.util.Map;

public interface CompanyMetadataApi {
    CompanyDto getCompanyById(int companyId);

    List<CompanyDto> getAllCompanies();

    List<String> getAllMarketIds();

    void createOrUpdateMarket(MarketDto market);

    void createOrUpdateIndustry(IndustryDto industry);

    void updateCompanyCiks(Map<String, String> tickerToCik);
}
