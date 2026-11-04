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
import com.oraculum.analyst.dto.valuation.ForwardReverseDcfDto;
import com.oraculum.analyst.dto.valuation.ImpliedEpsEntryDto;
import com.oraculum.analyst.dto.valuation.MatrixSummaryDto;
import com.oraculum.analyst.dto.valuation.MultipleAnchorDto;
import com.oraculum.analyst.dto.valuation.ReverseDcfSummaryDto;
import com.oraculum.analyst.dto.valuation.SensitivityMatrixDto;
import com.oraculum.analyst.dto.valuation.ValuationScenarioDto;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.SharePriceSignalDto;
import com.oraculum.company.api.dto.ValuationZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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

        ReverseDcfSummaryDto dcfSummary = ReverseDcfSummaryDto.builder()
                .baseCaseImpliedGrowthPct(16.68f)
                .baseCaseValuationZone("AGGRESSIVE")
                .minImpliedGrowthPct(9.5f)
                .maxImpliedGrowthPct(22.0f)
                .medianImpliedGrowthPct(18.5f)
                .aggressiveOrSpeculativeCellsPct(88)
                .historicalFcfCagrPct(83.1f)
                .build();
        SensitivityMatrixDto matrix = SensitivityMatrixDto.builder()
                .targetFiscalYear("2028-01-31")
                .epsLow(12.85f)
                .epsAvg(15.91f)
                .epsHigh(18.75f)
                .referenceMultiples(List.of(20.1f, 25.0f, 29.2f))
                .multipleAnchors(List.of(new MultipleAnchorDto(25.0f, "10Y Historical Min P/E (25.0x)")))
                .impliedEpsByMultiple(List.of(ImpliedEpsEntryDto.builder()
                        .multiple(25.0f)
                        .anchorLabel("10Y Historical Min P/E (25.0x)")
                        .requiredEpsForCurrentPrice(8.44f)
                        .consensusEpsAvg(15.91f)
                        .consensusVsRequiredPct(88.5f)
                        .build()))
                .matrixSummary(MatrixSummaryDto.builder()
                        .minPctMove(-23.6f)
                        .maxPctMove(160.2f)
                        .medianPctMove(88.6f)
                        .baseCaseMultiple(25.0f)
                        .baseCaseImpliedPrice(397.75f)
                        .baseCasePctMove(88.5f)
                        .breakEvenPeOnAvgEps(13.26f)
                        .build())
                .disclaimer("Disclaimer")
                .build();
        ValuationScenarioDto scenarios = ValuationScenarioDto.builder()
                .currentSharePrice(210.96f)
                .forwardPeFy1(22.66f)
                .forwardPeFy2(13.26f)
                .nextQuarterEndDate(LocalDate.of(2026, 10, 31))
                .nextQuarterEpsConsensus(2.47f)
                .nextQuarterRevenueConsensus(65000.0f)
                .flagEpsReconciliationDivergence(false)
                .epsReconciliationStatus("GAAP_NON_GAAP_BASIS_DIFF")
                .epsReconciliationNote("Basis diff note")
                .sensitivityMatrix(matrix)
                .reverseDcfSummary(dcfSummary)
                .flagReverseDcfPeDivergence(true)
                .valuationMethodTensionNote("METHOD CONFLICT: P/E 22.7x vs AGGRESSIVE DCF")
                .build();
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
        assertThat(summary.businessQuality()).isEqualTo("EXCEPTIONAL");
        assertThat(summary.valuationVsExpectations()).isEqualTo("DEMANDING");
        assertThat(summary.methodAgreement()).isEqualTo("CONFLICTING_PEAK_MARGIN_RISK");
        assertThat(summary.marketSetup()).isEqualTo("PULLBACK_IN_UPTREND");
        assertThat(summary.priceDate()).isEqualTo(LocalDate.of(2026, 10, 23));
        assertThat(summary.priceStalenessDays()).isEqualTo(5);
        assertThat(summary.requiredFcfCagrPct()).isEqualTo(16.68f);
        assertThat(summary.ttmFcfGrowthPct()).isEqualTo(76.5f);
        assertThat(summary.decisiveQuestion()).contains("16.7%");
        assertThat(summary.impliedEpsByMultiple()).hasSize(1);
    }

    @Test
    void buildDecisionSummaryAndDrivers_whenTwoStageDcfIsFair_resolvesHurdleToReasonableAndSupportive() {
        ReverseDcfSummaryDto dcfSummary = ReverseDcfSummaryDto.builder()
                .baseCaseImpliedGrowthPct(16.68f)
                .baseCaseValuationZone("AGGRESSIVE")
                .aggressiveOrSpeculativeCellsPct(100)
                .historicalFcfCagrPct(83.1f)
                .build();
        ForwardReverseDcfDto twoStage = ForwardReverseDcfDto.builder()
                .fy2FcfProxyUsd(145_000_000_000.0)
                .impliedGrowthYears3To10Pct(7.2f)
                .relativeValuationZone(ValuationZone.FAIR)
                .build();
        ValuationScenarioDto scenarios = ValuationScenarioDto.builder()
                .currentSharePrice(210.96f)
                .forwardPeFy1(22.66f)
                .reverseDcfSummary(dcfSummary)
                .twoStageReverseDcf(twoStage)
                .flagReverseDcfPeDivergence(false)
                .build();
        CompanyFinancialRatiosDto ttm = createRatio(StatementVariant.TTM, 0.765f, 0.99f, 56.1f, 48.8f, 0, 0);

        CompanyFactSheetData factSheet = CompanyFactSheetData.builder()
                .valuationScenarios(scenarios)
                .companyFinancialRatios(Map.of(StatementVariant.TTM, List.of(ttm)))
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheet)
                .state(state)
                .build();

        DecisionSummary summary = calculator.buildDecisionSummary(
                AnalysisResult.builder().valuation(ValuationAssessment.UNDERVALUED).build(), ctx
        );
        List<DecisionDriver> drivers = calculator.buildDecisionDrivers(ctx);

        assertThat(summary.valuationVsExpectations()).isEqualTo("REASONABLE");
        assertThat(summary.decisiveQuestion()).contains("$145.0B").contains("7.2%");
        assertThat(drivers.getFirst().factor()).isEqualTo("Implied FCF Growth Hurdle");
        assertThat(drivers.getFirst().status()).isEqualTo("SUPPORTIVE");
        assertThat(drivers.getFirst().benchmarkOrRequired()).contains("7.2% Y3-10 FCF CAGR (FAIR)");
    }

    @Test
    void buildDecisionDrivers_createsStructuredDriversWithTrafficLightStatuses() {
        ReverseDcfSummaryDto dcfSummary = ReverseDcfSummaryDto.builder()
                .baseCaseImpliedGrowthPct(16.68f)
                .baseCaseValuationZone("AGGRESSIVE")
                .aggressiveOrSpeculativeCellsPct(88)
                .historicalFcfCagrPct(83.1f)
                .build();
        ValuationScenarioDto scenarios = ValuationScenarioDto.builder()
                .currentSharePrice(210.96f)
                .forwardPeFy1(22.66f)
                .forwardPeFy2(13.26f)
                .nextQuarterEndDate(LocalDate.of(2026, 10, 31))
                .nextQuarterEpsConsensus(2.47f)
                .flagEpsReconciliationDivergence(false)
                .epsReconciliationStatus("ALIGNED")
                .reverseDcfSummary(dcfSummary)
                .flagReverseDcfPeDivergence(true)
                .valuationMethodTensionNote("METHOD CONFLICT")
                .build();
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
        assertThat(drivers.get(1).status()).isEqualTo("SUPPORTIVE");
        assertThat(drivers.get(2).factor()).isEqualTo("Cash Conversion & Working Capital (DSO)");
        assertThat(drivers.get(2).status()).isEqualTo("WARNING");
        assertThat(drivers.get(3).factor()).isEqualTo("Shareholder Payout & Debt Coverage");
        assertThat(drivers.get(3).status()).isEqualTo("RISK");
    }

    private SharePriceSignalDto createSignal(
            LocalDate tradeDate,
            float price,
            float ma50,
            float ma200,
            int distDays
    ) {
        return new SharePriceSignalDto(
                tradeDate, 1, "NVDA", "us", "N", "USD",
                price, 100000, 0.0f, -2.0f, 14.0f,
                price, ma50, ma200, 220.0f, 140.0f, -4.0f, 90.0f, distDays, 1.0f,
                2026, "Q2", tradeDate, 5.0e12f, 29.2f, 0.034f, 52.0f, 25.0f, 40.0f,
                null, null, 0, 5.0e12f, 25.0f, 0.5f, 0.6f, 0.5f, 3.0f, 0.2f, 0.019f,
                25.0f, 52.0f, 0.4f, 0.72f, 0.62f, 0.5f, 2.5f, 50.0f, null, 0,
                0.7f, 0.8f, 8, 4, 4, 4, 1, 0, 90.0f,
                null, null, null, null, null, 0, 0, 0, "NEUTRAL"
        );
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
