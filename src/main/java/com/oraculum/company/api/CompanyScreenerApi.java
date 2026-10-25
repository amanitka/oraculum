package com.oraculum.company.api;

import com.oraculum.company.api.dto.CompanyOverviewDto;
import com.oraculum.company.api.dto.ScreenerDto;
import com.oraculum.company.api.dto.ScreenerInsiderDto;

import java.util.List;

public interface CompanyScreenerApi {
    List<CompanyOverviewDto> getCompanyOverview();
    List<ScreenerDto> getUndervaluedScreener();
    List<ScreenerDto> getQualityCompoundersScreener();
    List<ScreenerDto> getGrahamDeepValueScreener();
    List<ScreenerDto> getFinancialTrendScreener();
    List<ScreenerInsiderDto> getInsiderScreener();
}
