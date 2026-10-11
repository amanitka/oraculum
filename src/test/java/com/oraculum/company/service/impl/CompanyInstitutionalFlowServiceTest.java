package com.oraculum.company.service.impl;

import com.oraculum.company.api.dto.CompanyInstitutionalFlowDto;
import com.oraculum.company.domain.CompanyInstitutionalFlowEntity;
import com.oraculum.company.repository.CompanyInstitutionalFlowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyInstitutionalFlowServiceTest {

    @Mock
    private CompanyInstitutionalFlowRepository repository;

    private CompanyInstitutionalFlowServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CompanyInstitutionalFlowServiceImpl(repository);
    }

    @Test
    void getLatestInstitutionalFlowByCompanyId_shouldReturnMapKeyedByCompanyId() {
        CompanyInstitutionalFlowEntity entity = createTestEntity(1, "AAPL", 1L);
        when(repository.findByQuarterIndex(1L)).thenReturn(List.of(entity));

        Map<Integer, CompanyInstitutionalFlowDto> result = service.getLatestInstitutionalFlowByCompanyId();

        assertThat(result).hasSize(1).containsKey(1);
        CompanyInstitutionalFlowDto dto = result.get(1);
        assertThat(dto.ticker()).isEqualTo("AAPL");
        assertThat(dto.sharesChangePct()).isEqualTo(10.5f);
        verify(repository).findByQuarterIndex(1L);
    }

    @Test
    void getInstitutionalFlowByPeriod_shouldReturnDataForGivenPeriod() {
        LocalDate period = LocalDate.of(2026, 3, 31);
        CompanyInstitutionalFlowEntity entity = createTestEntity(1, "AAPL", 2L);
        when(repository.findByReportPeriod(period)).thenReturn(List.of(entity));

        Map<Integer, CompanyInstitutionalFlowDto> result = service.getInstitutionalFlowByPeriod(period);

        assertThat(result).hasSize(1).containsKey(1);
        verify(repository).findByReportPeriod(period);
    }

    @Test
    void getAvailableReportPeriods_shouldReturnDistinctPeriods() {
        LocalDate p1 = LocalDate.of(2026, 6, 30);
        LocalDate p2 = LocalDate.of(2026, 3, 31);
        when(repository.findDistinctReportPeriods()).thenReturn(List.of(p1, p2));

        List<LocalDate> result = service.getAvailableReportPeriods();

        assertThat(result).containsExactly(p1, p2);
        verify(repository).findDistinctReportPeriods();
    }

    @Test
    void getInstitutionalFlowHistory_shouldReturnListOrderedByPeriod() {
        CompanyInstitutionalFlowEntity e1 = createTestEntity(1, "AAPL", 1L);
        CompanyInstitutionalFlowEntity e2 = createTestEntity(1, "AAPL", 2L);
        when(repository.findByCompanyIdOrderByReportPeriodDesc(1)).thenReturn(List.of(e1, e2));

        List<CompanyInstitutionalFlowDto> result = service.getInstitutionalFlowHistory(1);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).quarterIndex()).isEqualTo(1L);
        assertThat(result.get(1).quarterIndex()).isEqualTo(2L);
        verify(repository).findByCompanyIdOrderByReportPeriodDesc(1);
    }

    private CompanyInstitutionalFlowEntity createTestEntity(int companyId, String ticker, long quarterIndex) {
        CompanyInstitutionalFlowEntity entity = new CompanyInstitutionalFlowEntity();
        entity.setCompanyId(companyId);
        entity.setTicker(ticker);
        entity.setCompanyName("Apple Inc.");
        entity.setMarket("us");
        entity.setCusip("037833100");
        entity.setReportPeriod(LocalDate.of(2026, 6, 30));
        entity.setQuarterIndex(quarterIndex);
        entity.setTotalInstShares(1_000_000L);
        entity.setTotalInstValueUsd(150_000_000L);
        entity.setSharesChangePct(10.5f);
        entity.setNetSharesChange(100_000L);
        entity.setPutCallRatio(1.15f);
        entity.setTopHoldersNames(new String[]{"BlackRock", "Vanguard"});
        entity.setTier1BuyerNames(new String[]{"Citadel"});
        return entity;
    }
}
