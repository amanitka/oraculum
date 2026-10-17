package com.oraculum.analyst.service;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.CriticAgentOutput;
import com.oraculum.analyst.agent.dto.SpecialistOutput;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.api.domain.ValuationAssessment;
import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.ReverseDcfMatrixCellDto;
import com.oraculum.company.api.dto.ValuationZone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SynthesizerGuardrailService {

    public AnalysisResult applyGuardrails(
            AnalysisResult rawResult,
            AgentContext ctx,
            CriticAgentOutput criticOutput
    ) {
        if (rawResult == null) {
            return null;
        }
        AnalysisResult result = enforceValuationVerdictGuardrail(rawResult, ctx);
        return enforceConvictionGuardrail(result, ctx, criticOutput);
    }

    private AnalysisResult enforceValuationVerdictGuardrail(AnalysisResult result, AgentContext ctx) {
        if (ctx.factSheetData() == null) {
            return result;
        }
        ReverseDcfDto dcf = ctx.factSheetData().getReverseDcfResult();
        if (dcf == null || dcf.sensitivityMatrix() == null || dcf.sensitivityMatrix().cells() == null) {
            return result;
        }
        List<ReverseDcfMatrixCellDto> cells = dcf.sensitivityMatrix().cells();
        if (cells.isEmpty()) {
            return result;
        }
        long aggressiveOrSpeculative = countAggressiveOrSpeculativeCells(cells);
        double aggressiveRatio = (double) aggressiveOrSpeculative / cells.size();

        if (aggressiveRatio >= 0.70 && result.valuation() == ValuationAssessment.UNDERVALUED) {
            return adjustUndervaluedVerdict(result, aggressiveRatio);
        }
        return result;
    }

    private long countAggressiveOrSpeculativeCells(List<ReverseDcfMatrixCellDto> cells) {
        return cells.stream()
                .filter(c -> c.valuationZone() == ValuationZone.AGGRESSIVE || c.valuationZone() == ValuationZone.SPECULATIVE)
                .count();
    }

    private AnalysisResult adjustUndervaluedVerdict(AnalysisResult result, double ratio) {
        int pct = (int) Math.round(ratio * 100);
        String prefix = String.format(
                "[Guardrail Note: Valuation adjusted from UNDERVALUED to FAIRLY_VALUED because %d%% of Reverse DCF sensitivity cells require aggressive/speculative growth.] ",
                pct
        );
        String updatedReasoning = prefix + (result.recommendationReasoning() != null ? result.recommendationReasoning() : "");
        log.warn("Deterministic guardrail adjusted valuation verdict to FAIRLY_VALUED due to high hurdle ratio: {}%", pct);
        return result.toBuilder()
                .valuation(ValuationAssessment.FAIRLY_VALUED)
                .recommendationReasoning(updatedReasoning)
                .build();
    }

    private AnalysisResult enforceConvictionGuardrail(
            AnalysisResult result,
            AgentContext ctx,
            CriticAgentOutput criticOutput
    ) {
        List<Double> scores = extractSpecialistScores(ctx);
        if (scores.size() < 3) {
            return result;
        }
        double stdDev = calculateStandardDeviation(scores);
        int unresolved = criticOutput != null ? criticOutput.unresolvedContradictions().size() : 0;
        int targetConviction = calculateTargetConviction(stdDev, unresolved);

        log.info("Specialist scores stdDev: {}, unresolved contradictions: {}, calculated conviction: {} (raw: {})",
                stdDev, unresolved, targetConviction, result.conviction());

        return result.toBuilder()
                .conviction(targetConviction)
                .build();
    }

    private List<Double> extractSpecialistScores(AgentContext ctx) {
        List<Double> scores = new ArrayList<>();
        Map<AgentType, Object> outputs = ctx.state().getSpecialistOutputs();
        for (Object output : outputs.values()) {
            if (output instanceof SpecialistOutput specialist) {
                scores.add(specialist.score());
            }
        }
        return scores;
    }

    private double calculateStandardDeviation(List<Double> scores) {
        double mean = scores.stream().mapToDouble(Double::doubleValue).average().orElse(5.0);
        double variance = scores.stream().mapToDouble(s -> Math.pow(s - mean, 2)).average().orElse(0.0);
        return Math.sqrt(variance);
    }

    private int calculateTargetConviction(double stdDev, int unresolved) {
        if (stdDev < 1.0 && unresolved == 0) {
            return 5;
        }
        if (stdDev < 1.6 && unresolved <= 1) {
            return 4;
        }
        if (stdDev < 2.2 && unresolved <= 2) {
            return 3;
        }
        if (stdDev < 3.0) {
            return 2;
        }
        return 1;
    }
}
