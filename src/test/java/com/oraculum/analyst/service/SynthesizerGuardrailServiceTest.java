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
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.ReverseDcfMatrixCellDto;
import com.oraculum.company.api.dto.ReverseDcfSensitivityMatrixDto;
import com.oraculum.company.api.dto.ValuationZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SynthesizerGuardrailServiceTest {

    private SynthesizerGuardrailService guardrailService;

    @Mock
    private CompanyFactSheetData factSheetData;

    private AgentWorkflowState state;

    @BeforeEach
    void setUp() {
        guardrailService = new SynthesizerGuardrailService();
        state = new AgentWorkflowState();
    }

    @Test
    void applyGuardrails_aggressiveDcfHurdle_adjustsUndervaluedToFairlyValued() {
        List<ReverseDcfMatrixCellDto> cells = List.of(
                new ReverseDcfMatrixCellDto(10.0, 3.0, 18.0, ValuationZone.AGGRESSIVE, true),
                new ReverseDcfMatrixCellDto(11.0, 3.0, 21.0, ValuationZone.SPECULATIVE, false),
                new ReverseDcfMatrixCellDto(9.0, 3.0, 15.0, ValuationZone.AGGRESSIVE, false)
        ); // 3/3 = 100% aggressive/speculative
        ReverseDcfSensitivityMatrixDto matrix = new ReverseDcfSensitivityMatrixDto(List.of(10.0), List.of(3.0), cells);
        ReverseDcfDto dcf = new ReverseDcfDto(100.0, 10.0, 0.1, 0.1, 10, 0.03, 18.0, 12.0, "5Y", false, "High hurdle", matrix);
        when(factSheetData.getReverseDcfResult()).thenReturn(dcf);

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
        List<ReverseDcfMatrixCellDto> cells = List.of(
                new ReverseDcfMatrixCellDto(10.0, 3.0, 8.0, ValuationZone.FAIR, true),
                new ReverseDcfMatrixCellDto(9.0, 3.0, 6.0, ValuationZone.DEEP_DISCOUNT, false),
                new ReverseDcfMatrixCellDto(11.0, 3.0, 11.0, ValuationZone.FAIR, false)
        );
        ReverseDcfSensitivityMatrixDto matrix = new ReverseDcfSensitivityMatrixDto(List.of(10.0), List.of(3.0), cells);
        ReverseDcfDto dcf = new ReverseDcfDto(100.0, 10.0, 0.1, 0.1, 10, 0.03, 8.0, 12.0, "5Y", false, "Low hurdle", matrix);
        when(factSheetData.getReverseDcfResult()).thenReturn(dcf);

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
        // High consensus: 8.5, 8.7, 8.6 -> stdDev < 0.1
        state.putAgentOutput(AgentType.FUNDAMENTALS, StandardAgentOutput.builder().score(8.5).build());
        state.putAgentOutput(AgentType.VALUATION, StandardAgentOutput.builder().score(8.7).build());
        state.putAgentOutput(AgentType.CASH_FLOW, StandardAgentOutput.builder().score(8.6).build());

        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .conviction(1) // Raw model under-reported conviction
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .state(state)
                .build();

        AnalysisResult guarded = guardrailService.applyGuardrails(rawResult, ctx, null);

        assertThat(guarded.conviction()).isEqualTo(5);
    }

    @Test
    void applyGuardrails_reducesConvictionWhenUnresolvedContradictionsExist() {
        state.putAgentOutput(AgentType.FUNDAMENTALS, StandardAgentOutput.builder().score(9.0).build());
        state.putAgentOutput(AgentType.VALUATION, StandardAgentOutput.builder().score(5.0).build());
        state.putAgentOutput(AgentType.CASH_FLOW, StandardAgentOutput.builder().score(8.0).build());

        Contradiction unresolved = new Contradiction(
                "Unresolved margin divergence",
                List.of("FUNDAMENTALS", "VALUATION"),
                CorrectionType.DATA_SOURCE_CONFLICT,
                Resolution.UNRESOLVED
        );
        CriticAgentOutput criticOutput = new CriticAgentOutput(List.of(unresolved), false, List.of());

        AnalysisResult rawResult = AnalysisResult.builder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .conviction(5)
                .build();

        AgentContext ctx = AgentContext.builder()
                .factSheetData(factSheetData)
                .state(state)
                .build();

        AnalysisResult guarded = guardrailService.applyGuardrails(rawResult, ctx, criticOutput);

        assertThat(guarded.conviction()).isLessThanOrEqualTo(3);
    }
}
