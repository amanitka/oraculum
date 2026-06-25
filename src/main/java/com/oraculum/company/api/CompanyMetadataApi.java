package com.oraculum.company.api;

import com.oraculum.company.api.dto.CompanyDto;

import java.util.List;

public interface CompanyMetadataApi {
    CompanyDto getCompanyById(int companyId);
    List<CompanyDto> getAllCompanies();
    List<String> getAllMarketIds();
    
    void createOrUpdateMarket(com.oraculum.company.api.dto.MarketDto market);
    void createOrUpdateIndustry(com.oraculum.company.api.dto.IndustryDto industry);
}
