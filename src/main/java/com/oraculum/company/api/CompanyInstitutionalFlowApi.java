package com.oraculum.company.api;

import com.oraculum.company.api.dto.CompanyInstitutionalFlowDto;
import com.oraculum.company.api.dto.SecFilerDto;
import com.oraculum.company.api.dto.SectorInstitutionalFlowTrendDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CompanyInstitutionalFlowApi {

    Map<Integer, CompanyInstitutionalFlowDto> getLatestInstitutionalFlowByCompanyId();

    Map<Integer, CompanyInstitutionalFlowDto> getInstitutionalFlowByPeriod(LocalDate reportPeriod);

    List<LocalDate> getAvailableReportPeriods();

    List<CompanyInstitutionalFlowDto> getInstitutionalFlowHistory(int companyId);

    List<SectorInstitutionalFlowTrendDto> getSectorInstitutionalFlowByPeriod(LocalDate reportPeriod);
 
    List<SecFilerDto> getTier1Filers();
}
