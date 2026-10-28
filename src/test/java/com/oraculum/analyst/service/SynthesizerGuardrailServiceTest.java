package com.oraculum.analyst.service;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentWorkflowState;
import com.oraculum.analyst.agent.dto.CriticAgentOutput;
import com.oraculum.analyst.agent.dto.CriticAgentOutput.Contradiction;
import com.oraculum.analyst.agent.dto.CriticAgentOutput.CorrectionType;
import com.oraculum.analyst.agent.dto.CriticAgentOutput.Resolution;
import com.oraculum.analyst.agent.dto.StandardAgentOutput;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.api.domain.ValuationAssessment;
import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.ScenarioCase;
import com.oraculum.analyst.api.dto.AnalysisResult.ScenariosResult;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.analyst.dto.ValuationScenarioDto;
import com.oraculum.analyst.dto.ValuationScenarioDto.SensitivityMatrix;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.ReverseDcfMatrixCellDto;
import com.oraculum.company.api.dto.ReverseDcfSensitivityMatrixDto;
import com.oraculum.company.api.dto.ValuationZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SynthesizerGuardrailServiceTest {

    private SynthesizerGuardrailService guardrailService;
    private AgentWorkflowState state;

    @BeforeEach
    void setUp() {
        guardrailService = new SynthesizerGuardrailService();
        state = new AgentWorkflowState();
    }

    @Test
    void applyGuardrails_aggressiveDcfHurdle_adjustsUndervaluedToFairlyValued() {
        ReverseDcfDto dcf = createDcfWithCells(List.of(
                new ReverseDcfMatrixCellDto(10.0, 3.0, 18.0, ValuationZone.AGGRESSIVE, true),
                new ReverseDcfMatrixCellDto(11.0, 3.0, 21.0, ValuationZone.SPECULATIVE, false),
                new ReverseDcfMatrixCellDto(9.0, 3.0, 15.0, ValuationZone.AGGRESSIVE, false)
        ));
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .reverseDcfResult(dcf)
                .build();

        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.UNDERVALUED)
                .recommendationReasoning("Solid long-term outlook.")
                .conviction(4)
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .state(state)
                .build();

        AnalysisResult guarded = guardrailService.applyGuardrails(rawResult, ctx, null);

        assertThat(guarded.valuation()).isEqualTo(ValuationAssessment.FAIRLY_VALUED);
        assertThat(guarded.recommendationReasoning()).contains("[Guardrail Note:");
    }

    @Test
    void applyGuardrails_fairDcfHurdle_preservesUndervalued() {
        ReverseDcfDto dcf = createDcfWithCells(List.of(
                new ReverseDcfMatrixCellDto(10.0, 3.0, 8.0, ValuationZone.FAIR, true),
                new ReverseDcfMatrixCellDto(9.0, 3.0, 6.0, ValuationZone.DEEP_DISCOUNT, false),
                new ReverseDcfMatrixCellDto(11.0, 3.0, 11.0, ValuationZone.FAIR, false)
        ));
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .reverseDcfResult(dcf)
                .build();

        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.UNDERVALUED)
                .recommendationReasoning("Low hurdle supports value.")
                .conviction(4)
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .state(state)
                .build();

        AnalysisResult guarded = guardrailService.applyGuardrails(rawResult, ctx, null);

        assertThat(guarded.valuation()).isEqualTo(ValuationAssessment.UNDERVALUED);
        assertThat(guarded.recommendationReasoning()).doesNotContain("[Guardrail Note:");
    }

    @Test
    void applyGuardrails_derivesConvictionFromSpecialistDispersions() {
        setupSpecialistScores(8.5, 8.7, 8.6);

        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .conviction(1)
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(CompanyFactSheetData.builder().build())
                .state(state)
                .build();

        AnalysisResult guarded = guardrailService.applyGuardrails(rawResult, ctx, null);

        assertThat(guarded.conviction()).isEqualTo(5);
    }

    @Test
    void applyGuardrails_reducesConvictionWhenUnresolvedContradictionsExist() {
        setupSpecialistScores(9.0, 5.0, 8.0);
        CriticAgentOutput criticOutput = createCriticWithUnresolvedContradiction();

        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .conviction(5)
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(CompanyFactSheetData.builder().build())
                .state(state)
                .build();

        AnalysisResult guarded = guardrailService.applyGuardrails(rawResult, ctx, criticOutput);

        assertThat(guarded.conviction()).isLessThanOrEqualTo(3);
    }

    @Test
    void applyGuardrails_reverseDcfPeDivergence_capsConvictionAtThree() {
        setupSpecialistScores(8.5, 8.7, 8.6);
        ValuationScenarioDto scenarios = new ValuationScenarioDto(
                100.0f, 15.0f, 12.0f, null, null, null, false, null, null, true
        );
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .valuationScenarios(scenarios)
                .build();

        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .conviction(5)
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .state(state)
                .build();

        AnalysisResult guarded = guardrailService.applyGuardrails(rawResult, ctx, null);

        assertThat(guarded.conviction()).isEqualTo(3);
        assertThat(guarded.convictionReasons()).anyMatch(r -> r.contains("Valuation method conflict"));
        assertThat(guarded.decisionSummary()).isNotNull();
    }

    @Test
    void applyGuardrails_lowEarningsQuality_capsConvictionAtThree() {
        setupSpecialistScores(8.5, 8.7, 8.6);
        CompanyFinancialRatiosDto ratio = createRatioWithEarningsQuality(0.55f);
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .companyFinancialRatios(Map.of(StatementVariant.TTM, List.of(ratio)))
                .build();

        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .conviction(5)
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .state(state)
                .build();

        AnalysisResult guarded = guardrailService.applyGuardrails(rawResult, ctx, null);

        assertThat(guarded.conviction()).isEqualTo(3);
        assertThat(guarded.convictionReasons()).anyMatch(r -> r.contains("Low TTM earnings quality"));
        assertThat(guarded.decisionSummary()).isNotNull();
    }

    @Test
    void applyGuardrails_emptyFactorScores_populatesFromSpecialists() {
        state.putAgentOutput(AgentType.FUNDAMENTALS, StandardAgentOutput.builder().score(8.0).build());
        state.putAgentOutput(AgentType.VALUATION, StandardAgentOutput.builder().score(7.0).build());
        state.putAgentOutput(AgentType.EARNINGS_ESTIMATES, StandardAgentOutput.builder().score(6.5).build());
        state.putAgentOutput(AgentType.RISK, StandardAgentOutput.builder().score(7.5).build());

        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .factorScores(Map.of())
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(CompanyFactSheetData.builder().build())
                .state(state)
                .build();

        AnalysisResult guarded = guardrailService.applyGuardrails(rawResult, ctx, null);

        assertThat(guarded.factorScores()).isNotNull();
        assertThat(guarded.factorScores().get("fundamental_health")).isEqualTo(8.0);
        assertThat(guarded.factorScores().get("valuation")).isEqualTo(7.0);
        assertThat(guarded.factorScores().get("growth_prospects")).isEqualTo(6.5);
        assertThat(guarded.factorScores().get("risk_profile")).isEqualTo(7.5);
    }

    @Test
    void applyGuardrails_sanitizesAdviceWords_preservesCompoundWords() {
        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .thesis("We recommend to BUY the stock due to share buybacks and sell-through strength. Do not SELL.")
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(CompanyFactSheetData.builder().build())
                .state(state)
                .build();

        AnalysisResult guarded = guardrailService.applyGuardrails(rawResult, ctx, null);

        assertThat(guarded.thesis())
                .contains("favorable investment case")
                .contains("cautious positioning")
                .contains("share buybacks")
                .contains("sell-through");
    }

    @Test
    void applyGuardrails_preservesCommercialBuyAndSellPhrases() {
        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .thesis("OpenAI plans to buy Google chips while hyperscalers buy computing capacity and sell cloud services.")
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(CompanyFactSheetData.builder().build())
                .state(state)
                .build();

        AnalysisResult guarded = guardrailService.applyGuardrails(rawResult, ctx, null);

        assertThat(guarded.thesis())
                .contains("buy Google chips")
                .contains("buy computing capacity")
                .contains("sell cloud services")
                .doesNotContain("favorable investment case");
    }

    @Test
    void applyGuardrails_debtFundedPayout_capsConvictionAtThree() {
        setupSpecialistScores(8.5, 8.7, 8.6);
        LocalDate date = LocalDate.of(2026, 7, 31);
        CompanyFinancialRatiosDto ratio = new CompanyFinancialRatiosDto(
                "id", 1, "TEST", "USD", null, StatementVariant.QUARTERLY,
                2026, "Q2", date, date, date,
                1000f, 800f, 500f, 400f, 100f, 0.2f, 0.25f, 0.15f,
                0.2f, 5000f, 1000f, 0.4f, 0.25f, 0.16f, 1.5f,
                1.2f, 0.5f, 5.0f, 10f, 45f, 30f, 0.1f, 0.12f, 0.15f,
                0.08f, 0.1f, 8, 0.95f, 1, 0, 1,
                4, 4, 4, 8.5f,
                null, 120.8f, 0.0f, null, null, 0, 1, 1
        );
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .companyFinancialRatios(Map.of(StatementVariant.QUARTERLY, List.of(ratio)))
                .build();

        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .conviction(5)
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .state(state)
                .build();

        AnalysisResult guarded = guardrailService.applyGuardrails(rawResult, ctx, null);

        assertThat(guarded.conviction()).isEqualTo(3);
        assertThat(guarded.convictionReasons()).anyMatch(r -> r.contains("debt issuance"));
        assertThat(guarded.decisionSummary()).isNotNull();
        assertThat(guarded.decisionDrivers()).isNotEmpty();
    }

    @Test
    void applyGuardrails_factorScores_overwritesSynthesizerScoresWithSpecialists() {
        state.putAgentOutput(AgentType.FUNDAMENTALS, StandardAgentOutput.builder().score(9.0).build());
        state.putAgentOutput(AgentType.VALUATION, StandardAgentOutput.builder().score(8.0).build());
        state.putAgentOutput(AgentType.EARNINGS_ESTIMATES, StandardAgentOutput.builder().score(7.0).build());
        state.putAgentOutput(AgentType.RISK, StandardAgentOutput.builder().score(6.0).build());

        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .factorScores(Map.of("fundamental_health", 5.0, "valuation", 5.0, "growth_prospects", 5.0, "risk_profile", 5.0))
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(CompanyFactSheetData.builder().build())
                .state(state)
                .build();

        AnalysisResult guarded = guardrailService.applyGuardrails(rawResult, ctx, null);

        assertThat(guarded.factorScores().get("fundamental_health")).isEqualTo(9.0);
        assertThat(guarded.factorScores().get("valuation")).isEqualTo(8.0);
        assertThat(guarded.factorScores().get("growth_prospects")).isEqualTo(7.0);
        assertThat(guarded.factorScores().get("risk_profile")).isEqualTo(6.0);
    }

    @Test
    void applyGuardrails_factorScores_partialSpecialists_preservesMissingOrDefaults() {
        state.putAgentOutput(AgentType.FUNDAMENTALS, StandardAgentOutput.builder().score(8.5).build());

        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .factorScores(Map.of("valuation", 6.2))
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(CompanyFactSheetData.builder().build())
                .state(state)
                .build();

        AnalysisResult guarded = guardrailService.applyGuardrails(rawResult, ctx, null);

        assertThat(guarded.factorScores().get("fundamental_health")).isEqualTo(8.5);
        assertThat(guarded.factorScores().get("valuation")).isEqualTo(6.2);
        assertThat(guarded.factorScores().get("growth_prospects")).isEqualTo(5.0);
        assertThat(guarded.factorScores().get("risk_profile")).isEqualTo(5.0);
    }

    @Test
    void applyGuardrails_highEpsDispersion_capsConvictionAtThree() {
        setupSpecialistScores(8.5, 8.7, 8.6);
        SensitivityMatrix matrix = new SensitivityMatrix("FY26", 2.0f, 4.0f, 6.0f, List.of(), List.of(), null);
        ValuationScenarioDto scenarios = new ValuationScenarioDto(
                100.0f, 15.0f, 12.0f, null, null, null, false, null, matrix, false
        );
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .valuationScenarios(scenarios)
                .build();

        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .conviction(5)
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .state(state)
                .build();

        AnalysisResult guarded = guardrailService.applyGuardrails(rawResult, ctx, null);

        assertThat(guarded.conviction()).isEqualTo(3);
        assertThat(guarded.convictionReasons()).anyMatch(r -> r.contains("EPS estimate dispersion"));
        assertThat(guarded.decisionSummary()).isNotNull();
    }

    @Test
    void applyGuardrails_normalEpsDispersion_doesNotCapConviction() {
        setupSpecialistScores(8.5, 8.7, 8.6);
        SensitivityMatrix matrix = new SensitivityMatrix("FY26", 3.8f, 4.0f, 4.2f, List.of(), List.of(), null);
        ValuationScenarioDto scenarios = new ValuationScenarioDto(
                100.0f, 15.0f, 12.0f, null, null, null, false, null, matrix, false
        );
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .valuationScenarios(scenarios)
                .build();

        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .conviction(5)
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .state(state)
                .build();

        AnalysisResult guarded = guardrailService.applyGuardrails(rawResult, ctx, null);

        assertThat(guarded.conviction()).isEqualTo(5);
    }

    @Test
    void applyGuardrails_sanitizesAllSnapshotFieldsAndScenarios() {
        ScenariosResult scenarios = new ScenariosResult(
                new ScenarioCase("Strong buy driver", "Expect strong buy flow", "BUY upside", "None", "None"),
                new ScenarioCase("Base sell pressure", "Key sell level", "None", "Hold or sell", "None"),
                new ScenarioCase("Bear strong sell panic", "Margin hit sell", "None", "None", "SELL downside")
        );
        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .whatWouldChangeThis("If management issues a SELL warning")
                .topBullPoints(List.of("Catalyst to BUY now"))
                .topBearPoints(List.of("Reason to SELL soon"))
                .thesisBreakers(List.of("Triggers a strong sell condition"))
                .scenarios(scenarios)
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(CompanyFactSheetData.builder().build())
                .state(state)
                .build();

        AnalysisResult guarded = guardrailService.applyGuardrails(rawResult, ctx, null);

        assertThat(guarded.whatWouldChangeThis()).contains("cautious positioning");
        assertThat(guarded.topBullPoints().getFirst()).contains("favorable investment case");
        assertThat(guarded.topBearPoints().getFirst()).contains("cautious positioning");
        assertThat(guarded.thesisBreakers().getFirst()).contains("cautious positioning");
        assertThat(guarded.scenarios().bull().description()).contains("favorable investment case");
        assertThat(guarded.scenarios().base().keyAssumption()).contains("cautious positioning");
        assertThat(guarded.scenarios().bear().impliedDownside()).contains("cautious positioning");
    }

    private void setupSpecialistScores(double s1, double s2, double s3) {
        state.putAgentOutput(AgentType.FUNDAMENTALS, StandardAgentOutput.builder().score(s1).build());
        state.putAgentOutput(AgentType.VALUATION, StandardAgentOutput.builder().score(s2).build());
        state.putAgentOutput(AgentType.CASH_FLOW, StandardAgentOutput.builder().score(s3).build());
    }

    private ReverseDcfDto createDcfWithCells(List<ReverseDcfMatrixCellDto> cells) {
        ReverseDcfSensitivityMatrixDto matrix = new ReverseDcfSensitivityMatrixDto(List.of(10.0), List.of(3.0), cells);
        return new ReverseDcfDto(100.0, 10.0, 0.1, 0.1, 10, 0.03, 18.0, 12.0, "5Y", false, "Hurdle test", matrix);
    }

    private CriticAgentOutput createCriticWithUnresolvedContradiction() {
        Contradiction unresolved = new Contradiction(
                "Unresolved margin divergence",
                List.of("FUNDAMENTALS", "VALUATION"),
                CorrectionType.DATA_SOURCE_CONFLICT,
                Resolution.UNRESOLVED
        );
        return new CriticAgentOutput(List.of(unresolved), false, List.of());
    }

    private CompanyFinancialRatiosDto createRatioWithEarningsQuality(float eqRatio) {
        LocalDate date = LocalDate.of(2026, 1, 1);
        return new CompanyFinancialRatiosDto(
                "id", 1, "TEST", "USD", null, StatementVariant.TTM,
                2026, "Q3", date, date, date,
                1000f, 800f, 500f, 400f, 100f, 0.2f, 0.25f, 0.15f,
                0.2f, 5000f, 1000f, 0.4f, 0.25f, 0.16f, 1.5f,
                1.2f, 0.5f, 5.0f, 10f, 45f, 30f, 0.1f, 0.12f, 0.15f,
                0.08f, 0.1f, 8, eqRatio, 1, 0, 1,
                4, 4, 4, 8.5f,
                null, null, null, null, null, 0, 0, 0
        );
    }
}
