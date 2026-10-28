package com.oraculum.analyst.service;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.CriticAgentOutput;
import com.oraculum.analyst.agent.dto.SpecialistOutput;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.api.domain.ValuationAssessment;
import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.AnalysisResult.ScenarioCase;
import com.oraculum.analyst.api.dto.AnalysisResult.ScenariosResult;
import com.oraculum.analyst.dto.ValuationScenarioDto;
import com.oraculum.analyst.dto.ValuationScenarioDto.SensitivityMatrix;
import com.oraculum.analyst.service.valuation.DecisionSummaryCalculator;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyFinancialRatiosDto;
import com.oraculum.company.api.dto.ReverseDcfDto;
import com.oraculum.company.api.dto.ReverseDcfMatrixCellDto;
import com.oraculum.company.api.dto.ValuationZone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
public class SynthesizerGuardrailService {

    private static final Pattern ADVICE_WORD_PATTERN = Pattern.compile(
            "(?<![a-zA-Z0-9-])\\b(strong buy|strong sell|buy|sell)\\b(?![a-zA-Z0-9-])" +
            "(?!(?:\\s+[a-zA-Z0-9'-]+){0,4}\\s+\\b(?:chips|silicon|gpus|cpus|processors|products|capacity|compute|cloud|infrastructure|networking|equipment|hardware|software|services|components|memory|systems|servers|licenses|supplies|goods|ads|advertising|semiconductors|accelerators|wafers|inventory|assets|businesses|companies)\\b)",
            Pattern.CASE_INSENSITIVE
    );

    private final DecisionSummaryCalculator decisionSummaryCalculator;

    @Autowired
    public SynthesizerGuardrailService(DecisionSummaryCalculator decisionSummaryCalculator) {
        this.decisionSummaryCalculator = decisionSummaryCalculator;
    }

    public SynthesizerGuardrailService() {
        this(new DecisionSummaryCalculator());
    }

    public AnalysisResult applyGuardrails(
            AnalysisResult rawResult,
            AgentContext ctx,
            CriticAgentOutput criticOutput
    ) {
        if (rawResult == null) {
            return null;
        }
        AnalysisResult result = enforceValuationVerdictGuardrail(rawResult, ctx);
        result = enforceFactorScores(result, ctx);
        result = enforceConvictionGuardrail(result, ctx, criticOutput);
        result = populateDecisionSupportLayer(result, ctx);
        return sanitizeComplianceLanguage(result);
    }

    private AnalysisResult populateDecisionSupportLayer(AnalysisResult result, AgentContext ctx) {
        if (decisionSummaryCalculator == null) {
            return result;
        }
        return result.toBuilder()
                .decisionSummary(decisionSummaryCalculator.buildDecisionSummary(result, ctx))
                .decisionDrivers(decisionSummaryCalculator.buildDecisionDrivers(ctx))
                .build();
    }

    private AnalysisResult enforceValuationVerdictGuardrail(AnalysisResult result, AgentContext ctx) {
        if (ctx == null || ctx.factSheetData() == null) {
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
        List<String> reasons = new ArrayList<>();
        int targetConviction = result.conviction();

        if (scores.size() >= 3) {
            double stdDev = calculateStandardDeviation(scores);
            int unresolved = criticOutput != null ? criticOutput.unresolvedContradictions().size() : 0;
            targetConviction = calculateTargetConviction(stdDev, unresolved);
            reasons.add(String.format(Locale.ROOT,
                    "Base conviction %d/5 from specialist score dispersion (stdDev %.2f) and %d unresolved contradictions",
                    targetConviction, stdDev, unresolved));
            targetConviction = applyConvictionCaps(targetConviction, ctx, reasons);
            log.info("Specialist scores stdDev: {}, unresolved contradictions: {}, calculated conviction: {} (raw: {})",
                    stdDev, unresolved, targetConviction, result.conviction());
        } else {
            targetConviction = applyConvictionCaps(targetConviction, ctx, reasons);
        }

        return result.toBuilder()
                .conviction(targetConviction)
                .convictionReasons(reasons)
                .build();
    }

    private int applyConvictionCaps(int conviction, AgentContext ctx, List<String> reasons) {
        if (ctx == null || ctx.factSheetData() == null) {
            return conviction;
        }
        int capped = conviction;
        ValuationScenarioDto scenarios = ctx.factSheetData().getValuationScenariosObject();
        if (scenarios != null && Boolean.TRUE.equals(scenarios.flagReverseDcfPeDivergence())) {
            capped = Math.min(capped, 3);
            reasons.add("Capped at 3/5: Valuation method conflict between moderate P/E and demanding Reverse DCF growth hurdle");
        }
        if (isHighEpsDispersion(scenarios)) {
            capped = Math.min(capped, 3);
            reasons.add("Capped at 3/5: High analyst EPS estimate dispersion (>35% spread)");
        }
        Float eq = extractEarningsQuality(ctx);
        if (eq != null && eq < 0.70f) {
            capped = Math.min(capped, 3);
            reasons.add(String.format(Locale.ROOT, "Capped at 3/5: Low TTM earnings quality ratio (%.1f%% OCF/NI < 70.0%%)", eq * 100.0f));
        }
        if (hasDebtFundedPayoutFlag(ctx)) {
            capped = Math.min(capped, 3);
            reasons.add("Capped at 3/5: Shareholder payout exceeded organic FCF with concurrent debt issuance");
        }
        return capped;
    }

    private boolean hasDebtFundedPayoutFlag(AgentContext ctx) {
        if (ctx.factSheetData() == null) {
            return false;
        }
        return isLatestDebtFunded(ctx.factSheetData().getCompanyFinancialRatiosList(StatementVariant.TTM))
                || isLatestDebtFunded(ctx.factSheetData().getCompanyFinancialRatiosList(StatementVariant.QUARTERLY));
    }

    private boolean isLatestDebtFunded(List<CompanyFinancialRatiosDto> list) {
        if (list == null || list.isEmpty() || list.getFirst() == null) {
            return false;
        }
        return Integer.valueOf(1).equals(list.getFirst().flagDebtFundedPayout());
    }

    private boolean isHighEpsDispersion(ValuationScenarioDto scenarios) {
        if (scenarios == null || scenarios.sensitivityMatrix() == null) {
            return false;
        }
        SensitivityMatrix matrix = scenarios.sensitivityMatrix();
        Float low = matrix.epsLow();
        Float avg = matrix.epsAvg();
        Float high = matrix.epsHigh();
        if (low == null || avg == null || high == null || avg <= 0) {
            return false;
        }
        return ((high - low) / avg) > 0.35f;
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
        Map<String, Double> scores = result.factorScores() != null
                ? new LinkedHashMap<>(result.factorScores())
                : new LinkedHashMap<>();

        setSpecialistScoreIfPresent(scores, "fundamental_health", ctx, AgentType.FUNDAMENTALS);
        setSpecialistScoreIfPresent(scores, "valuation", ctx, AgentType.VALUATION);
        setSpecialistScoreIfPresent(scores, "growth_prospects", ctx, AgentType.EARNINGS_ESTIMATES);
        setSpecialistScoreIfPresent(scores, "risk_profile", ctx, AgentType.RISK);

        return result.toBuilder().factorScores(scores).build();
    }

    private void setSpecialistScoreIfPresent(Map<String, Double> scores, String key, AgentContext ctx, AgentType type) {
        Double specialistScore = getSpecialistScoreOrNull(ctx, type);
        if (specialistScore != null) {
            scores.put(key, specialistScore);
        } else if (!scores.containsKey(key)) {
            scores.put(key, 5.0);
        }
    }

    private Double getSpecialistScoreOrNull(AgentContext ctx, AgentType type) {
        if (ctx == null || ctx.state() == null) {
            return null;
        }
        Object output = ctx.state().getAgentOutput(type);
        if (output instanceof SpecialistOutput spec) {
            return spec.score();
        }
        return null;
    }

    private AnalysisResult sanitizeComplianceLanguage(AnalysisResult result) {
        return result.toBuilder()
                .thesis(sanitizeText(result.thesis()))
                .executiveSummary(sanitizeText(result.executiveSummary()))
                .recommendationReasoning(sanitizeText(result.recommendationReasoning()))
                .valuationOneLiner(sanitizeText(result.valuationOneLiner()))
                .whatWouldChangeThis(sanitizeText(result.whatWouldChangeThis()))
                .topBullPoints(sanitizeList(result.topBullPoints()))
                .topBearPoints(sanitizeList(result.topBearPoints()))
                .thesisBreakers(sanitizeList(result.thesisBreakers()))
                .scenarios(sanitizeScenarios(result.scenarios()))
                .build();
    }

    private List<String> sanitizeList(List<String> list) {
        if (list == null) {
            return null;
        }
        return list.stream().map(this::sanitizeText).toList();
    }

    private ScenariosResult sanitizeScenarios(ScenariosResult scenarios) {
        if (scenarios == null) {
            return null;
        }
        return new ScenariosResult(
                sanitizeScenarioCase(scenarios.bull()),
                sanitizeScenarioCase(scenarios.base()),
                sanitizeScenarioCase(scenarios.bear())
        );
    }

    private ScenarioCase sanitizeScenarioCase(ScenarioCase c) {
        if (c == null) {
            return null;
        }
        return new ScenarioCase(
                sanitizeText(c.description()),
                sanitizeText(c.keyAssumption()),
                sanitizeText(c.impliedUpside()),
                sanitizeText(c.impliedMove()),
                sanitizeText(c.impliedDownside())
        );
    }

    private String sanitizeText(String text) {
        if (text == null) return null;
        return ADVICE_WORD_PATTERN.matcher(text).replaceAll(mr -> {
            int start = mr.start();
            String prefix = text.substring(Math.max(0, start - 25), start).toLowerCase(Locale.ROOT);
            if (isCommercialPrefix(prefix)) {
                return mr.group(0);
            }
            String word = mr.group(1).toLowerCase(Locale.ROOT);
            return switch (word) {
                case "buy", "strong buy" -> "favorable investment case";
                case "sell", "strong sell" -> "cautious positioning";
                default -> mr.group(0);
            };
        });
    }

    private static boolean isCommercialPrefix(String prefix) {
        return prefix.endsWith("to ")
                && (prefix.contains("plan") || prefix.contains("agree") || prefix.contains("promise") || prefix.contains("intend"));
    }

    private List<Double> extractSpecialistScores(AgentContext ctx) {
        if (ctx == null || ctx.state() == null) {
            return List.of();
        }
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
