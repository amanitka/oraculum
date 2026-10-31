package com.oraculum.company.service.impl;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.company.api.dto.CompanyInstitutionalFlowDto;
import com.oraculum.company.api.dto.SecFilerDto;
import com.oraculum.company.api.dto.SectorInstitutionalFlowTrendDto;
import com.oraculum.company.domain.CompanyInstitutionalFlowEntity;
import com.oraculum.company.domain.SecFilerEntity;
import com.oraculum.company.domain.SectorInstitutionalFlowEntity;
import com.oraculum.company.repository.CompanyInstitutionalFlowRepository;
import com.oraculum.company.repository.SecFilerRepository;
import com.oraculum.company.repository.SectorInstitutionalFlowRepository;
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

    @Mock
    private SectorInstitutionalFlowRepository sectorRepository;

    @Mock
    private SecFilerRepository secFilerRepository;

    private OraculumProperties properties;

    private CompanyInstitutionalFlowServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new OraculumProperties(null, null, null, null);
        service = new CompanyInstitutionalFlowServiceImpl(repository, sectorRepository, secFilerRepository, properties);
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
        assertThat(dto.capitalFlowUsd()).isEqualTo(15_000_000L);
        assertThat(dto.priceValuationEffectUsd()).isEqualTo(5_000_000L);
        assertThat(dto.flagIncompleteFilerSample()).isFalse();
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

    @Test
    void getSectorInstitutionalFlowByPeriod_shouldReturnDtosFromRepository() {
        LocalDate period = LocalDate.of(2026, 6, 30);
        SectorInstitutionalFlowEntity entity =
                new SectorInstitutionalFlowEntity(
                        "Technology", period, 500_000_000_000L, 25_000_000_000L, 15_000_000_000L, 10_000_000_000L, 10_000_000L, 500_000L, 50L);
        when(sectorRepository.findByReportPeriodOrderByNetValueChangeUsdDesc(period)).thenReturn(List.of(entity));

        List<SectorInstitutionalFlowTrendDto> result = service.getSectorInstitutionalFlowByPeriod(period);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().sectorName()).isEqualTo("Technology");
        assertThat(result.getFirst().totalInstValueUsd()).isEqualTo(500_000_000_000L);
        assertThat(result.getFirst().capitalFlowUsd()).isEqualTo(15_000_000_000L);
        assertThat(result.getFirst().flowPct()).isEqualTo(3.0f);
        assertThat(result.getFirst().companiesCount()).isEqualTo(50L);
        verify(sectorRepository).findByReportPeriodOrderByNetValueChangeUsdDesc(period);
    }

    @Test
    void getTier1Filers_shouldReturnMappedDtosFromRepository() {
        SecFilerEntity f1 = new SecFilerEntity("0001067983", "Berkshire Hathaway Inc", true, (short) 1, LocalDate.of(2026, 8, 14));
        SecFilerEntity f2 = new SecFilerEntity("0001423053", "CITADEL ADVISORS LLC", true, (short) 1, LocalDate.of(2026, 8, 15));
        when(secFilerRepository.findByTierOrderByManagerNameAsc((short) 1)).thenReturn(List.of(f1, f2));

        List<SecFilerDto> result = service.getTier1Filers();

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().cik()).isEqualTo("0001067983");
        assertThat(result.getFirst().managerName()).isEqualTo("Berkshire Hathaway Inc");
        assertThat(result.get(1).cik()).isEqualTo("0001423053");
    }

    @Test
    void getTier1Filers_shouldPopulateIconicInvestorFromProperties() {
        SecFilerEntity f1 = new SecFilerEntity("0001067983", "Berkshire Hathaway Inc", true, (short) 1, LocalDate.of(2026, 8, 14));
        when(secFilerRepository.findByTierOrderByManagerNameAsc((short) 1)).thenReturn(List.of(f1));

        OraculumProperties.Data.Sec13F sec13f = new OraculumProperties.Data.Sec13F(null, List.of("0001067983"), Map.of("0001067983", "Warren Buffett"));
        OraculumProperties.Data data = new OraculumProperties.Data(null, null, sec13f, null);
        properties = new OraculumProperties(data, null, null, null);
        service = new CompanyInstitutionalFlowServiceImpl(repository, sectorRepository, secFilerRepository, properties);

        List<SecFilerDto> result = service.getTier1Filers();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().cik()).isEqualTo("0001067983");
        assertThat(result.getFirst().iconicInvestor()).isEqualTo("Warren Buffett");
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
        entity.setCapitalFlowUsd(15_000_000L);
        entity.setPriceValuationEffectUsd(5_000_000L);
        entity.setFlagIncompleteFilerSample(false);
        entity.setPutCallRatio(1.15f);
        entity.setTopHoldersNames(new String[]{"BlackRock", "Vanguard"});
        entity.setTier1BuyerNames(new String[]{"Citadel"});
        return entity;
    }
}
