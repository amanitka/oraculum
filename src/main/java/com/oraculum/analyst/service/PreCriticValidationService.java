package com.oraculum.analyst.service;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.CriticAgentOutput.Contradiction;
import com.oraculum.analyst.agent.dto.CriticAgentOutput.CorrectionType;
import com.oraculum.analyst.agent.dto.CriticAgentOutput.Resolution;
import com.oraculum.analyst.agent.dto.StandardAgentOutput;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class PreCriticValidationService {

    public List<Contradiction> validate(AgentContext ctx) {
        List<Contradiction> findings = new ArrayList<>();
        checkDcfAgainstValuationScore(ctx, findings);
        checkFiscalYearCalendarAlignment(ctx, findings);
        checkSmartMoneyAttribution(ctx, findings);
        checkMarginExpansionSignalAlignment(ctx, findings);
        return findings;
    }

    private void checkDcfAgainstValuationScore(AgentContext ctx, List<Contradiction> findings) {
        if (ctx.factSheetData() == null) {
            return;
        }
        ReverseDcfDto dcf = ctx.factSheetData().getReverseDcfResult();
        if (dcf == null || dcf.sensitivityMatrix() == null || dcf.sensitivityMatrix().cells() == null) {
            return;
        }
        Optional<ReverseDcfMatrixCellDto> baseCase = dcf.sensitivityMatrix().cells().stream()
                .filter(ReverseDcfMatrixCellDto::isBaseCase)
                .findFirst();
        if (baseCase.isEmpty()) {
            return;
        }
        ReverseDcfMatrixCellDto cell = baseCase.get();
        if (cell.valuationZone() == ValuationZone.AGGRESSIVE || cell.valuationZone() == ValuationZone.SPECULATIVE) {
            evaluateValuationScoreContradiction(ctx, cell, findings);
        }
    }

    private void evaluateValuationScoreContradiction(
            AgentContext ctx,
            ReverseDcfMatrixCellDto cell,
            List<Contradiction> findings
    ) {
        Object valObj = ctx.state().getAgentOutput(AgentType.VALUATION);
        if (!(valObj instanceof StandardAgentOutput valOutput)) {
            return;
        }
        if (valOutput.score() >= 7.0) {
            String desc = String.format(
                    "Valuation agent assigned high score (%.1f) and attractive stance despite Reverse DCF baseline requiring %.1f%% FCF growth (%s zone).",
                    valOutput.score(),
                    cell.impliedFcfGrowthPct(),
                    cell.valuationZone().displayName()
            );
            findings.add(new Contradiction(
                    desc,
                    List.of(AgentType.VALUATION.name()),
                    CorrectionType.REASONING_ERROR,
                    Resolution.RERUN_APPLIED
            ));
        }
    }

    private void checkFiscalYearCalendarAlignment(AgentContext ctx, List<Contradiction> findings) {
        if (ctx.factSheetData() == null) {
            return;
        }
        List<CompanyFinancialRatiosDto> ratios = ctx.factSheetData().getCompanyFinancialRatiosList(StatementVariant.TTM);
        if (ratios.isEmpty()) {
            ratios = ctx.factSheetData().getCompanyFinancialRatiosList(StatementVariant.ANNUAL);
        }
        CompanyFinancialRatiosDto latest = ratios.stream()
                .filter(r -> r.reportDate() != null)
                .max(Comparator.comparing(CompanyFinancialRatiosDto::reportDate))
                .orElse(null);
        if (latest == null || latest.reportDate() == null) {
            return;
        }
        if (latest.reportDate().getYear() != latest.fiscalYear()) {
            String desc = String.format(
                    "Fiscal calendar shift: Financial statements use fiscal year FY%d for period ending %s (calendar year %d). Unify to calendar-anchored dates.",
                    latest.fiscalYear(),
                    latest.reportDate(),
                    latest.reportDate().getYear()
            );
            findings.add(new Contradiction(
                    desc,
                    List.of(AgentType.FUNDAMENTALS.name(), AgentType.NEWS.name()),
                    CorrectionType.TIME_WINDOW_MISMATCH,
                    Resolution.NARRATIVE_RECONCILED
            ));
        }
    }

    private void checkSmartMoneyAttribution(AgentContext ctx, List<Contradiction> findings) {
        Object smObj = ctx.state().getAgentOutput(AgentType.SMART_MONEY);
        if (!(smObj instanceof StandardAgentOutput smOutput)) {
            return;
        }
        List<CompanyInstitutionalFlowDto> flows = ctx.factSheetData() != null
                ? ctx.factSheetData().getInstitutionalFlowHistory()
                : List.of();
        if (flows == null || flows.isEmpty()) {
            verifyAccumulationClaimsWithoutFlows(smOutput, findings);
        }
    }

    private void verifyAccumulationClaimsWithoutFlows(
            StandardAgentOutput smOutput,
            List<Contradiction> findings
    ) {
        String narrative = buildSmartMoneyNarrative(smOutput).toLowerCase(Locale.ROOT);
        boolean claimsAccumulation = narrative.contains("increased")
                || narrative.contains("accumulated")
                || narrative.contains("bought")
                || narrative.contains("inflow");
        if (claimsAccumulation) {
            findings.add(new Contradiction(
                    "Smart Money agent asserts institutional accumulation without quarterly share change (delta) data.",
                    List.of(AgentType.SMART_MONEY.name()),
                    CorrectionType.UNSUPPORTED_CLAIM,
                    Resolution.NARRATIVE_RECONCILED
            ));
        }
    }

    private String buildSmartMoneyNarrative(StandardAgentOutput smOutput) {
        StringBuilder sb = new StringBuilder();
        if (smOutput.headline() != null) sb.append(smOutput.headline()).append(" ");
        if (smOutput.summary() != null) sb.append(smOutput.summary()).append(" ");
        if (smOutput.strengths() != null) sb.append(String.join(" ", smOutput.strengths()));
        return sb.toString();
    }

    private void checkMarginExpansionSignalAlignment(AgentContext ctx, List<Contradiction> findings) {
        if (ctx.factSheetData() == null || ctx.state() == null) {
            return;
        }
        Object fundObj = ctx.state().getAgentOutput(AgentType.FUNDAMENTALS);
        if (!(fundObj instanceof StandardAgentOutput fundOutput)) {
            return;
        }
        List<CompanyFinancialRatiosDto> ttmRatios = ctx.factSheetData().getCompanyFinancialRatiosList(StatementVariant.TTM);
        if (ttmRatios == null || ttmRatios.isEmpty() || ttmRatios.getFirst() == null) {
            return;
        }
        Integer actualTtmSignal = ttmRatios.getFirst().marginExpansionSignal();
        if (actualTtmSignal == null) {
            return;
        }
        evaluateNarrativeMarginSignalContradiction(fundOutput, actualTtmSignal, findings);
    }

    private void evaluateNarrativeMarginSignalContradiction(StandardAgentOutput fundOutput, int actualSignal, List<Contradiction> findings) {
        String narrative = buildFundamentalsNarrative(fundOutput).toLowerCase(Locale.ROOT);
        boolean mentionsZeroAtTtm = narrative.contains("margin expansion signal is not positive at annual/ttm")
                || narrative.contains("margin_expansion_signal was 0 at ttm")
                || (narrative.contains("margin_expansion_signal was 0") && narrative.contains("ttm") && !narrative.contains("annual margin_expansion_signal was 0"));

        if (actualSignal == 1 && mentionsZeroAtTtm) {
            findings.add(new Contradiction(
                    "Fundamentals agent asserted margin_expansion_signal was not positive at TTM level, whereas TTM financial ratios report marginExpansionSignal = 1.",
                    List.of(AgentType.FUNDAMENTALS.name()),
                    CorrectionType.UNSUPPORTED_CLAIM,
                    Resolution.NARRATIVE_RECONCILED
            ));
        }
    }

    private String buildFundamentalsNarrative(StandardAgentOutput output) {
        StringBuilder sb = new StringBuilder();
        if (output.headline() != null) sb.append(output.headline()).append(" ");
        if (output.summary() != null) sb.append(output.summary()).append(" ");
        if (output.strengths() != null) sb.append(String.join(" ", output.strengths())).append(" ");
        if (output.weaknesses() != null) sb.append(String.join(" ", output.weaknesses()));
        return sb.toString();
    }
}
