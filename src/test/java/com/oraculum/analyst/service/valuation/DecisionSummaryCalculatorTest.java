package com.oraculum.analyst.service.valuation;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentWorkflowState;
import com.oraculum.analyst.agent.dto.StandardAgentOutput;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.api.domain.ValuationAssessment;
import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionDriver;
import com.oraculum.analyst.api.dto.AnalysisResult.DecisionSummary;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.analyst.dto.ValuationScenarioDto;
import com.oraculum.analyst.dto.ValuationScenarioDto.ImpliedEpsEntry;
import com.oraculum.analyst.dto.ValuationScenarioDto.MatrixSummary;
import com.oraculum.analyst.dto.ValuationScenarioDto.MultipleAnchor;
import com.oraculum.analyst.dto.ValuationScenarioDto.ReverseDcfSummary;
import com.oraculum.analyst.dto.ValuationScenarioDto.SensitivityMatrix;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DecisionSummaryCalculatorTest {

    private DecisionSummaryCalculator calculator;
    private AgentWorkflowState state;

    @BeforeEach
    void setUp() {
        calculator = new DecisionSummaryCalculator();
        state = new AgentWorkflowState();
    }

    @Test
    void buildDecisionSummary_populatesAllFourDimensionsAndImpliedExpectations() {
        state.putAgentOutput(AgentType.FUNDAMENTALS, StandardAgentOutput.builder().score(9.2).build());
        state.putAgentOutput(AgentType.CASH_FLOW, StandardAgentOutput.builder().score(7.8).build());
        state.putAgentOutput(AgentType.SHARE_PRICE, StandardAgentOutput.builder().score(6.5).build());

        ReverseDcfSummary dcfSummary = new ReverseDcfSummary(
                16.68f, "AGGRESSIVE", 9.5f, 22.0f, 18.5f, 80, 83.1f
        );
        SensitivityMatrix matrix = new SensitivityMatrix(
                "2028-01-31", 12.85f, 15.91f, 18.75f,
                List.of(20.1f, 25.0f, 29.2f),
                List.of(new MultipleAnchor(25.0f, "10Y Historical Min P/E (25.0x)")),
                List.of(new ImpliedEpsEntry(25.0f, "10Y Historical Min P/E (25.0x)", 8.44f, 15.91f, 88.5f)),
                new MatrixSummary(-23.6f, 160.2f, 88.6f, 25.0f, 397.75f, 88.5f, 13.26f),
                List.of(),
                "Disclaimer"
        );
        ValuationScenarioDto scenarios = new ValuationScenarioDto(
                210.96f, 22.66f, 13.26f,
                LocalDate.of(2026, 10, 31), 2.47f, 65000.0f,
                false, "GAAP_NON_GAAP_BASIS_DIFF", "Basis diff note",
                matrix, dcfSummary, true, "METHOD CONFLICT: P/E 22.7x vs AGGRESSIVE DCF"
        );
        SharePriceSignalDto signal = createSignal(LocalDate.of(2026, 10, 23), 210.96f, 215.0f, 185.0f, 1);
        CompanyFinancialRatiosDto ttm = createRatio(StatementVariant.TTM, 0.765f, 0.99f, 56.1f, 48.8f, 0, 0);

        CompanyFactSheetData factSheet = CompanyFactSheetData.builder()
                .valuationScenarios(scenarios)
                .dailySharePriceSignals(List.of(signal))
                .companyFinancialRatios(Map.of(StatementVariant.TTM, List.of(ttm)))
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheet)
                .analysisDate(LocalDate.of(2026, 10, 28))
                .state(state)
                .build();

        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .build();

        DecisionSummary summary = calculator.buildDecisionSummary(rawResult, ctx);

        assertThat(summary).isNotNull();
        assertThat(summary.businessQuality()).isEqualTo("EXCEPTIONAL"); // avg(9.2, 7.8) = 8.5 >= 8.0
        assertThat(summary.valuationVsExpectations()).isEqualTo("DEMANDING");
        assertThat(summary.methodAgreement()).isEqualTo("CONFLICTING_PEAK_MARGIN_RISK");
        assertThat(summary.marketSetup()).isEqualTo("PULLBACK_IN_UPTREND"); // below MA50 (215), above MA200 (185)
        assertThat(summary.priceDate()).isEqualTo(LocalDate.of(2026, 10, 23));
        assertThat(summary.priceStalenessDays()).isEqualTo(5);
        assertThat(summary.requiredFcfCagrPct()).isEqualTo(16.68f);
        assertThat(summary.ttmFcfGrowthPct()).isEqualTo(76.5f);
        assertThat(summary.decisiveQuestion()).contains("16.7%");
        assertThat(summary.impliedEpsByMultiple()).hasSize(1);
    }

    @Test
    void buildDecisionDrivers_createsStructuredDriversWithTrafficLightStatuses() {
        ReverseDcfSummary dcfSummary = new ReverseDcfSummary(
                16.68f, "AGGRESSIVE", 9.5f, 22.0f, 18.5f, 80, 83.1f
        );
        ValuationScenarioDto scenarios = new ValuationScenarioDto(
                210.96f, 22.66f, 13.26f,
                LocalDate.of(2026, 10, 31), 2.47f, null,
                false, "ALIGNED", null,
                null, dcfSummary, true, "METHOD CONFLICT"
        );
        CompanyFinancialRatiosDto ttm = createRatio(StatementVariant.TTM, 0.765f, 0.99f, 56.1f, 48.8f, 0, 0);
        CompanyFinancialRatiosDto qtr = createRatio(StatementVariant.QUARTERLY, 0.50f, 0.40f, 62.2f, 120.8f, 1, 1);

        CompanyFactSheetData factSheet = CompanyFactSheetData.builder()
                .valuationScenarios(scenarios)
                .companyFinancialRatios(Map.of(
                        StatementVariant.TTM, List.of(ttm),
                        StatementVariant.QUARTERLY, List.of(qtr)
                ))
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheet)
                .state(state)
                .build();

        List<DecisionDriver> drivers = calculator.buildDecisionDrivers(ctx);

        assertThat(drivers).hasSize(4);
        assertThat(drivers.get(0).factor()).isEqualTo("Implied FCF Growth Hurdle");
        assertThat(drivers.get(0).status()).isEqualTo("DEMANDING");
        assertThat(drivers.get(1).factor()).isEqualTo("Forward Earnings vs. Implied EPS");
        assertThat(drivers.get(1).status()).isEqualTo("WARNING");
        assertThat(drivers.get(2).factor()).isEqualTo("Cash Conversion & Working Capital (DSO)");
        assertThat(drivers.get(2).status()).isEqualTo("WARNING"); // Qtr OCF/NI 0.40 < 0.70
        assertThat(drivers.get(3).factor()).isEqualTo("Shareholder Payout & Debt Coverage");
        assertThat(drivers.get(3).status()).isEqualTo("RISK"); // Qtr flagDebtFundedPayout == 1
    }

    private SharePriceSignalDto createSignal(
            LocalDate tradeDate,
            float price,
            float ma50,
            float ma200,
            int distDays
    ) {
        SharePriceSignalDto signal = mock(SharePriceSignalDto.class);
        when(signal.tradeDate()).thenReturn(tradeDate);
        when(signal.sharePrice()).thenReturn(price);
        when(signal.ma50()).thenReturn(ma50);
        when(signal.ma200()).thenReturn(ma200);
        when(signal.downDaysHighVol5d()).thenReturn(distDays);
        return signal;
    }

    private CompanyFinancialRatiosDto createRatio(
            StatementVariant variant,
            float fcfGrowth,
            float eqRatio,
            float dso,
            float payoutPct,
            int flagExceedsFcf,
            int flagDebtPayout
    ) {
        LocalDate date = LocalDate.of(2026, 7, 31);
        return new CompanyFinancialRatiosDto(
                "id-" + variant, 1, "NVDA", "USD", null, variant,
                2026, "Q2", date, date, date,
                1000f, 800f, 500f, 400f, 100f, 0.2f, 0.25f, 0.15f,
                0.2f, 5000f, 1000f, 0.4f, 0.25f, 0.16f, 1.5f,
                1.2f, 0.5f, 5.0f, 10f, dso, 30f, 0.1f, 0.12f, 0.15f,
                fcfGrowth, 0.1f, 8, eqRatio, 1, 0, 1,
                4, 4, 4, 8.5f,
                null, payoutPct, 0.0f, null, null, 0, flagExceedsFcf, flagDebtPayout
        );
    }
}
