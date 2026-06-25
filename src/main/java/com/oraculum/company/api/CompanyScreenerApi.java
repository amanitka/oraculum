package com.oraculum.company.api;

import com.oraculum.company.api.dto.*;

import java.util.List;

public interface CompanyScreenerApi {
    List<ScreenerMasterDto> getMasterScreener();
    List<ScreenerNewsSentimentDto> getNewsSentimentScreener();
    List<ScreenerDto> getUndervaluedScreener();
    List<ScreenerDto> getQualityCompoundersScreener();
    List<ScreenerDto> getGrahamDeepValueScreener();
    List<ScreenerDto> getFinancialTrendScreener();
    List<ScreenerInsiderDto> getInsiderScreener();
}
