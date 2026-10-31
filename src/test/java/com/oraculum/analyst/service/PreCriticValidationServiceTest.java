package com.oraculum.analyst.service;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentWorkflowState;
import com.oraculum.analyst.agent.dto.CriticAgentOutput.Contradiction;
import com.oraculum.analyst.agent.dto.CriticAgentOutput.CorrectionType;
import com.oraculum.analyst.agent.dto.CriticAgentOutput.Resolution;
import com.oraculum.analyst.agent.dto.StandardAgentOutput;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.CompanyInstitutionalFlowDto;
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

class PreCriticValidationServiceTest {

    private PreCriticValidationService validationService;
    private AgentWorkflowState state;

    @BeforeEach
    void setUp() {
        validationService = new PreCriticValidationService();
        state = new AgentWorkflowState();
    }

    @Test
    void validate_aggressiveDcfWithHighScore_detectsReasoningError() {
        ReverseDcfMatrixCellDto baseCell = new ReverseDcfMatrixCellDto(
                10.0, 3.0, 18.5, ValuationZone.AGGRESSIVE, true
        );
        ReverseDcfSensitivityMatrixDto matrix = new ReverseDcfSensitivityMatrixDto(
                List.of(10.0), List.of(3.0), List.of(baseCell)
        );
        ReverseDcfDto dcf = new ReverseDcfDto(
                1000.0, 50.0, 0.05, 10.0, 10, 3.0, 18.5, 15.0, "5Y", false, "Hurdle high", matrix
        );
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .reverseDcfResult(dcf)
                .build();

        StandardAgentOutput valOutput = StandardAgentOutput.builder()
                .score(7.8)
                .headline("Attractive entry point with strong upside")
                .build();
        state.putAgentOutput(AgentType.VALUATION, valOutput);

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .state(state)
                .build();

        List<Contradiction> findings = validationService.validate(ctx);

        assertThat(findings).hasSize(1);
        Contradiction c = findings.getFirst();
        assertThat(c.correctionType()).isEqualTo(CorrectionType.REASONING_ERROR);
        assertThat(c.resolution()).isEqualTo(Resolution.RERUN_APPLIED);
        assertThat(c.agentsInvolved()).contains("VALUATION");
    }

    @Test
    void validate_fiscalYearShift_detectsTimeWindowMismatch() {
        LocalDate reportDate = LocalDate.of(2025, 7, 31);
        CompanyFinancialRatiosDto ratio = createRatio(2026, reportDate, null);
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .companyFinancialRatios(Map.of(StatementVariant.TTM, List.of(ratio)))
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .state(state)
                .build();

        List<Contradiction> findings = validationService.validate(ctx);

        assertThat(findings).hasSize(1);
        Contradiction c = findings.getFirst();
        assertThat(c.correctionType()).isEqualTo(CorrectionType.TIME_WINDOW_MISMATCH);
        assertThat(c.resolution()).isEqualTo(Resolution.NARRATIVE_RECONCILED);
        assertThat(c.description()).contains("FY2026", "2025");
    }

    @Test
    void validate_smartMoneyClaimsAccumulationWithoutFlows_detectsUnsupportedClaim() {
        StandardAgentOutput smOutput = StandardAgentOutput.builder()
                .score(6.5)
                .headline("BlackRock and Vanguard increased positions")
                .summary("Top funds accumulated heavily during the quarter")
                .build();
        state.putAgentOutput(AgentType.SMART_MONEY, smOutput);
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .institutionalFlowHistory(List.of())
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .state(state)
                .build();

        List<Contradiction> findings = validationService.validate(ctx);

        assertThat(findings).hasSize(1);
        Contradiction c = findings.getFirst();
        assertThat(c.correctionType()).isEqualTo(CorrectionType.UNSUPPORTED_CLAIM);
        assertThat(c.resolution()).isEqualTo(Resolution.NARRATIVE_RECONCILED);
    }

    @Test
    void validate_marginExpansionSignalConflict_detectsUnsupportedClaim() {
        StandardAgentOutput fundOutput = StandardAgentOutput.builder()
                .score(9.0)
                .headline("Exceptional operational growth")
                .summary("Margin expansion signal is not positive at annual/TTM level despite gross margin expansion.")
                .build();
        state.putAgentOutput(AgentType.FUNDAMENTALS, fundOutput);

        CompanyFinancialRatiosDto ratio = createRatio(2026, LocalDate.of(2026, 7, 31), 1);
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .companyFinancialRatios(Map.of(StatementVariant.TTM, List.of(ratio)))
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .state(state)
                .build();

        List<Contradiction> findings = validationService.validate(ctx);

        assertThat(findings).anyMatch(c ->
                c.agentsInvolved().contains("FUNDAMENTALS")
                        && c.correctionType() == CorrectionType.UNSUPPORTED_CLAIM
                        && c.description().contains("margin_expansion_signal was not positive at TTM level")
        );
    }

    @Test
    void validate_smartMoneyIncompleteSampleWithHighConfidence_detectsOverconfidence() {
        StandardAgentOutput.ConfidenceScore confidence = new StandardAgentOutput.ConfidenceScore(0.9, 0.9, 0.9);
        StandardAgentOutput smOutput = StandardAgentOutput.builder()
                .score(6.5)
                .confidence(confidence)
                .headline("Moderate institutional flow")
                .summary("Institutional positions remained relatively steady.")
                .build();
        state.putAgentOutput(AgentType.SMART_MONEY, smOutput);

        CompanyInstitutionalFlowDto flow = createTestFlow(true);
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .institutionalFlowHistory(List.of(flow))
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .state(state)
                .build();

        List<Contradiction> findings = validationService.validate(ctx);

        assertThat(findings).anyMatch(c ->
                c.agentsInvolved().contains("SMART_MONEY")
                        && c.correctionType() == CorrectionType.OVERCONFIDENCE
                        && c.description().contains("flag_incomplete_filer_sample")
        );
    }

    private CompanyInstitutionalFlowDto createTestFlow(boolean flagIncomplete) {
        return new CompanyInstitutionalFlowDto(
                1, "TEST", "Test Inc", "US", "Tech", "Semis", "000000000",
                LocalDate.of(2026, 6, 30), 1L, 100_000_000L, 1_000_000L, 50,
                List.of(), 50_000_000L, 50.0f, 10_000_000L, 20_000_000L, 0.5f,
                5_000_000L, 50_000L, 5.0f, 5_000_000L, 0L, flagIncomplete,
                20, 10, 5, 2, 10,
                10_000_000L, 100_000L, 5, 1_000_000L, 10_000L,
                3, 1, 1, 0, List.of(), List.of()
        );
    }

    private CompanyFinancialRatiosDto createRatio(int fiscalYear, LocalDate reportDate, Integer marginExpansionSignal) {
        return new CompanyFinancialRatiosDto(
                "test-id", 1, "TEST", "USD", null, StatementVariant.TTM,
                fiscalYear, "FY", reportDate, reportDate, reportDate,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                marginExpansionSignal, null, null, null, null,
                null, null, null, null, null, null, null, null
        );
    }
}
