package com.oraculum.company.api;

import com.oraculum.company.api.dto.CompanyInstitutionalFlowDto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CompanyInstitutionalFlowApi {

    Map<Integer, CompanyInstitutionalFlowDto> getLatestInstitutionalFlowByCompanyId();

    Map<Integer, CompanyInstitutionalFlowDto> getInstitutionalFlowByPeriod(LocalDate reportPeriod);

    List<LocalDate> getAvailableReportPeriods();

    List<CompanyInstitutionalFlowDto> getInstitutionalFlowHistory(int companyId);
}
