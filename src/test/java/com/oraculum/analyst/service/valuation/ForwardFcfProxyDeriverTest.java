package com.oraculum.analyst.service.valuation;

import com.oraculum.analyst.dto.valuation.ForwardFcfProxyRequest;
import com.oraculum.analyst.dto.valuation.MultiYearFcfProxyResult;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.harvester.api.dto.EarningsEstimateDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ForwardFcfProxyDeriverTest {

    private ForwardFcfProxyDeriver deriver;

    @BeforeEach
    void setUp() {
        deriver = new ForwardFcfProxyDeriver();
    }

    @Test
    void derive_bothPathsAgreeWithin15Pct_averagesBothPathsAndNoDivergenceFlag() {
        EarningsEstimateDto fy1 = createEstimate(LocalDate.of(2025, 12, 31), 100_000_000_000.0, 4.0);
        EarningsEstimateDto fy2 = createEstimate(LocalDate.of(2026, 12, 31), 120_000_000_000.0, 5.0);

        List<CompanyFinancialRatiosDto> annual = List.of(
                createRatio(0.40f, 40_000_000_000.0f, 40_000_000_000.0f),
                createRatio(0.40f, 40_000_000_000.0f, 40_000_000_000.0f)
        );
        CompanyFinancialRatiosDto ttm = createRatio(0.40f, 40_000_000_000.0f, 40_000_000_000.0f);

        ForwardFcfProxyRequest request = ForwardFcfProxyRequest.builder()
                .fy1(fy1)
                .fy2(fy2)
                .annualRatios(annual)
                .ttmRatio(ttm)
                .dilutedShares(10_000_000_000.0)
                .build();
        MultiYearFcfProxyResult result = deriver.derive(request);

        assertThat(result).isNotNull();
        assertThat(result.fy1FcfProxy()).isCloseTo(40_000_000_000.0, within(1000.0));
        assertThat(result.flagDivergence()).isFalse();
        assertThat(result.maxDivergencePct()).isLessThanOrEqualTo(15.1f);
    }

    @Test
    void derive_pathsDivergeBetween15And30Pct_selectsConservativeWithoutFlag() {
        EarningsEstimateDto fy1 = createEstimate(LocalDate.of(2025, 12, 31), 120_000_000_000.0, 4.0);
        List<CompanyFinancialRatiosDto> annual = List.of(createRatio(0.40f, 40_000_000_000.0f, 40_000_000_000.0f));

        ForwardFcfProxyRequest request = ForwardFcfProxyRequest.builder()
                .fy1(fy1)
                .annualRatios(annual)
                .dilutedShares(10_000_000_000.0)
                .build();
        MultiYearFcfProxyResult result = deriver.derive(request);

        assertThat(result.fy1FcfProxy()).isEqualTo(40_000_000_000.0);
        assertThat(result.flagDivergence()).isFalse();
        assertThat(result.maxDivergencePct()).isCloseTo(20.0f, within(0.01f));
    }

    @Test
    void derive_pathsDivergeMoreThan30Pct_selectsConservativeAndSetsDivergenceFlag() {
        EarningsEstimateDto fy1 = createEstimate(LocalDate.of(2025, 12, 31), 150_000_000_000.0, 4.0);
        List<CompanyFinancialRatiosDto> annual = List.of(createRatio(0.40f, 40_000_000_000.0f, 40_000_000_000.0f));

        ForwardFcfProxyRequest request = ForwardFcfProxyRequest.builder()
                .fy1(fy1)
                .annualRatios(annual)
                .dilutedShares(10_000_000_000.0)
                .build();
        MultiYearFcfProxyResult result = deriver.derive(request);

        assertThat(result.fy1FcfProxy()).isEqualTo(40_000_000_000.0);
        assertThat(result.flagDivergence()).isTrue();
        assertThat(result.maxDivergencePct()).isCloseTo(50.0f, within(0.01f));
    }

    @Test
    void derive_onlyOnePathAvailable_usesAvailablePathWithoutDivergence() {
        EarningsEstimateDto fy1 = createEstimate(LocalDate.of(2025, 12, 31), 100_000_000_000.0, null);
        List<CompanyFinancialRatiosDto> annual = List.of(createRatio(0.25f, null, null));

        ForwardFcfProxyRequest request = ForwardFcfProxyRequest.builder()
                .fy1(fy1)
                .annualRatios(annual)
                .dilutedShares(1_000_000.0)
                .build();
        MultiYearFcfProxyResult result = deriver.derive(request);

        assertThat(result.fy1FcfProxy()).isEqualTo(25_000_000_000.0);
        assertThat(result.flagDivergence()).isFalse();
    }

    @Test
    void derive_nullEstimates_returnsNullProxies() {
        ForwardFcfProxyRequest request = ForwardFcfProxyRequest.builder()
                .dilutedShares(100.0)
                .build();
        MultiYearFcfProxyResult result = deriver.derive(request);

        assertThat(result.fy1FcfProxy()).isNull();
        assertThat(result.fy2FcfProxy()).isNull();
        assertThat(result.flagDivergence()).isFalse();
    }

    private EarningsEstimateDto createEstimate(LocalDate date, Double revAvg, Double epsAvg) {
        BigDecimal rev = revAvg != null ? BigDecimal.valueOf(revAvg) : null;
        BigDecimal eps = epsAvg != null ? BigDecimal.valueOf(epsAvg) : null;
        return new EarningsEstimateDto(
                date, "fiscal year", eps, null, null,
                null, null, null, null, null, null, null, null, null,
                rev, null, null, null
        );
    }

    private CompanyFinancialRatiosDto createRatio(Float fcfMargin, Float fcf, Float netIncome) {
        return new CompanyFinancialRatiosDto(
                "ratio-1", 1, "NVDA", "USD", null, StatementVariant.ANNUAL,
                2025, "FY", LocalDate.of(2025, 1, 31), LocalDate.of(2025, 1, 31), LocalDate.of(2025, 1, 31),
                null, fcf, null, null, 10_000_000_000.0f, null, null, null,
                null, 100_000_000_000.0f, netIncome, null, 0.50f, fcfMargin,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, 0, 0, 0
        );
    }
}
