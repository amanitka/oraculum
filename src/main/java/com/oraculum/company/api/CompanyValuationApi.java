package com.oraculum.company.api;

import com.oraculum.company.api.dto.HistoricalValuationSummaryDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import java.util.List;

public interface CompanyValuationApi {
    
    ReverseDcfDto calculateReverseDcf(int companyId);
    
    List<HistoricalValuationSummaryDto> calculateHistoricalValuationPercentiles(int companyId);
}
