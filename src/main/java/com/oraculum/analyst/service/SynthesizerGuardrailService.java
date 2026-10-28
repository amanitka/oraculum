package com.oraculum.analyst.service;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.CriticAgentOutput;
import com.oraculum.analyst.agent.dto.SpecialistOutput;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.api.domain.ValuationAssessment;
import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.dto.ValuationScenarioDto;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.ReverseDcfMatrixCellDto;
import com.oraculum.company.api.dto.ValuationZone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
public class SynthesizerGuardrailService {

    private static final Pattern ADVICE_WORD_PATTERN = Pattern.compile(
            "(?<![a-zA-Z0-9-])\\b(buy|strong buy|sell|strong sell)\\b(?![a-zA-Z0-9-])",
            Pattern.CASE_INSENSITIVE
    );

    public AnalysisResult applyGuardrails(
            AnalysisResult rawResult,
            AgentContext ctx,
            CriticAgentOutput criticOutput
    ) {
        if (rawResult == null) {
            return null;
        }
        AnalysisResult result = enforceValuationVerdictGuardrail(rawResult, ctx);
        result = enforceConvictionGuardrail(result, ctx, criticOutput);
        result = enforceFactorScores(result, ctx);
        return sanitizeComplianceLanguage(result);
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

        if (ctx.factSheetData() != null) {
            ValuationScenarioDto scenarios = ctx.factSheetData().getValuationScenariosObject();
            if (scenarios != null && Boolean.TRUE.equals(scenarios.flagReverseDcfPeDivergence())) {
                targetConviction = Math.min(targetConviction, 3);
            }
            Float eq = extractEarningsQuality(ctx);
            if (eq != null && eq < 0.70f) {
                targetConviction = Math.min(targetConviction, 3);
            }
        }

        log.info("Specialist scores stdDev: {}, unresolved contradictions: {}, calculated conviction: {} (raw: {})",
                stdDev, unresolved, targetConviction, result.conviction());

        return result.toBuilder()
                .conviction(targetConviction)
                .build();
    }

    private Float extractEarningsQuality(AgentContext ctx) {
        if (ctx.factSheetData() == null) return null;
        List<CompanyFinancialRatiosDto> ttm = ctx.factSheetData().getCompanyFinancialRatiosList(StatementVariant.TTM);
        if (ttm != null && !ttm.isEmpty() && ttm.getFirst() != null) {
            return ttm.getFirst().earningsQualityRatio();
        }
        return null;
    }

    private AnalysisResult enforceFactorScores(AnalysisResult result, AgentContext ctx) {
        Map<String, Double> scores = result.factorScores();
        if (scores == null || scores.isEmpty()) {
            scores = new LinkedHashMap<>();
            scores.put("fundamental_health", getSpecialistScore(ctx, AgentType.FUNDAMENTALS));
            scores.put("valuation", getSpecialistScore(ctx, AgentType.VALUATION));
            scores.put("growth_prospects", getSpecialistScore(ctx, AgentType.EARNINGS_ESTIMATES));
            scores.put("risk_profile", getSpecialistScore(ctx, AgentType.RISK));
            return result.toBuilder().factorScores(scores).build();
        }
        return result;
    }

    private double getSpecialistScore(AgentContext ctx, AgentType type) {
        Object output = ctx.state().getAgentOutput(type);
        if (output instanceof SpecialistOutput spec) {
            return spec.score();
        }
        return 5.0;
    }

    private AnalysisResult sanitizeComplianceLanguage(AnalysisResult result) {
        String thesis = sanitizeText(result.thesis());
        String execSummary = sanitizeText(result.executiveSummary());
        String reasoning = sanitizeText(result.recommendationReasoning());
        String valuationOneLiner = sanitizeText(result.valuationOneLiner());

        return result.toBuilder()
                .thesis(thesis)
                .executiveSummary(execSummary)
                .recommendationReasoning(reasoning)
                .valuationOneLiner(valuationOneLiner)
                .build();
    }

    private String sanitizeText(String text) {
        if (text == null) return null;
        return ADVICE_WORD_PATTERN.matcher(text).replaceAll(mr -> {
            String word = mr.group(1).toLowerCase();
            return switch (word) {
                case "buy", "strong buy" -> "favorable investment case";
                case "sell", "strong sell" -> "cautious positioning";
                default -> mr.group(0);
            };
        });
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
