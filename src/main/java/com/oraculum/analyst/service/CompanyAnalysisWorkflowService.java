package com.oraculum.analyst.service;

import com.oraculum.analyst.agent.dto.*;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.api.domain.AnalysisStatus;
import com.oraculum.analyst.api.dto.CompanyAnalysisRequestEvent;
import com.oraculum.analyst.config.AnalystProperties;
import com.oraculum.analyst.dto.CompanyAnalysisResult;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.CompanyApi;
import com.oraculum.company.api.domain.StatementVariant;
import com.oraculum.company.api.dto.CompanyDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyAnalysisWorkflowService {

    private final CompanyApi companyApi;
    private final AnalystProperties analystProperties;
    private final CompanyFactSheetDataService companyFactSheetDataService;
    private final Map<AgentType, Agent<?>> agents;

    public CompanyAnalysisResult run(CompanyAnalysisRequestEvent request) {
        long startMs = System.currentTimeMillis();
        ZonedDateTime now = ZonedDateTime.now();

        log.info("Starting analysis workflow for ticker {}", request.ticker());
        try {
            AgentContext ctx = initializeContext(request);

            runPlannerPhase(request, ctx);
            runSpecialistPhase(ctx);
            runCriticCorrectionLoop(ctx);
            SynthesizerAgentOutput finalOutput = runSynthesizerPhase(ctx);

            return createSuccessResult(request, ctx, finalOutput, startMs, now);
        } catch (Exception e) {
            log.error("Workflow failed after {}ms: {}", System.currentTimeMillis() - startMs, e.getMessage(), e);
            LocalDate analysisDate = request.analysisDate() != null ? request.analysisDate() : LocalDate.now();
            return createFailureResult(request, analysisDate, e, now); // Can't easily recover partial state here unless we pass it up
        }
    }

    private AgentContext initializeContext(CompanyAnalysisRequestEvent request) {
        CompanyDto company = companyApi.getCompanyById(request.companyId());
        if (company == null) {
            throw new IllegalArgumentException("Company not found for ticker: " + request.ticker());
        }
        CompanyFactSheetData factSheetData = companyFactSheetDataService.create(company);
        LocalDate analysisDate = request.analysisDate() != null ? request.analysisDate() : LocalDate.now();

        return new AgentContext(company, factSheetData, analysisDate, request.statementVariant(), analystProperties.tokenBudget(), new AgentWorkflowState());
    }

    private void runPlannerPhase(CompanyAnalysisRequestEvent request, AgentContext ctx) {
        log.info("Starting Planner phase");
        @SuppressWarnings("unchecked")
        Agent<PlannerPlan> planner = (Agent<PlannerPlan>) agents.get(AgentType.PLANNER);
        var output = planner.run(ctx);
        PlannerPlan plan = output.result();

        recordTraceAndTokens(ctx, AgentType.PLANNER.name(), output);
        log.info("Planner phase complete. Plan: {}", plan);

        ctx.state().setAnalysisFocus(plan.getAnalysisFocus());
        ctx.state().setStatementVariants(getAgentStatementVariants(request, plan));
    }

    private void runSpecialistPhase(AgentContext ctx) {
        List<Agent<?>> specialists = Arrays.stream(AgentType.values())
                .filter(AgentType::isSpecialist)
                .sorted(Comparator.comparingInt(AgentType::getExecutionOrder))
                .map(agents::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        for (Agent<?> agent : specialists) {
            log.info("Starting {} phase", agent.getName());
            var output = agent.run(ctx);
            ctx.state().putAgentOutput(agent.getName(), output.result());
            recordTraceAndTokens(ctx, agent.getName().name(), output);
            log.info("{} phase complete. Tokens: {}", agent.getName(), output.tokens());
        }
    }

    private void runCriticCorrectionLoop(AgentContext ctx) {
        int maxReruns = analystProperties.critic().maxReruns();
        int maxSpecialistsPerRerun = analystProperties.critic().maxSpecialistsPerRerun();
        Agent<?> critic = agents.get(AgentType.CRITIC);

        for (int rerunCount = 0; rerunCount <= maxReruns; rerunCount++) {
            log.info("Starting Critic phase (rerunCount: {})", rerunCount);
            var outputRaw = critic.run(ctx);
            CriticAgentOutput output = (CriticAgentOutput) outputRaw.result();

            String traceKey = rerunCount == 0 ? "CRITIC" : "CRITIC_VERIFY_" + rerunCount;
            recordTraceAndTokens(ctx, traceKey, outputRaw);
            ctx.state().putAgentOutput(AgentType.CRITIC, output);
            log.info("Critic phase complete. Consistent: {}", output.isConsistent());

            if (output.isConsistent() || rerunCount == maxReruns) {
                break;
            }

            List<CriticAgentOutput.RerunInstruction> toRerun = getTopReruns(output, maxSpecialistsPerRerun);
            if (toRerun.isEmpty()) {
                break;
            }

            executeSpecialistReruns(ctx, toRerun, rerunCount);
        }
    }

    private List<CriticAgentOutput.RerunInstruction> getTopReruns(CriticAgentOutput output, int limit) {
        if (output.recommendedReruns() == null) {
            return List.of();
        }
        return output.recommendedReruns().stream()
                .sorted(Comparator.comparingInt(CriticAgentOutput.RerunInstruction::severity))
                .limit(limit)
                .toList();
    }

    private void executeSpecialistReruns(AgentContext ctx, List<CriticAgentOutput.RerunInstruction> reruns, int rerunCount) {
        log.info("Critic triggered rerun {} for: {}", rerunCount + 1, reruns.stream().map(CriticAgentOutput.RerunInstruction::specialist).toList());

        Map<AgentType, String> feedbackMap = reruns.stream()
                .collect(Collectors.toMap(CriticAgentOutput.RerunInstruction::specialist, CriticAgentOutput.RerunInstruction::instruction));
        ctx.state().setCriticFeedback(feedbackMap);

        List<CriticAgentOutput.RerunInstruction> orderedReruns = reruns.stream()
                .sorted(Comparator.comparingInt(r -> r.specialist().getExecutionOrder()))
                .toList();

        for (CriticAgentOutput.RerunInstruction instruction : orderedReruns) {
            AgentType type = instruction.specialist();
            Agent<?> agent = agents.get(type);
            if (agent != null) {
                log.info("Re-running {} phase based on Critic feedback", type);
                var output = agent.run(ctx);
                ctx.state().putAgentOutput(type, output.result());
                recordTraceAndTokens(ctx, type.name() + "_RERUN_" + rerunCount, output);
            }
        }
        ctx.state().clearCriticFeedback();
    }

    private SynthesizerAgentOutput runSynthesizerPhase(AgentContext ctx) {
        log.info("Starting Synthesizer phase");
        @SuppressWarnings("unchecked")
        Agent<SynthesizerAgentOutput> synthesizer = (Agent<SynthesizerAgentOutput>) agents.get(AgentType.SYNTHESIZER);
        var output = synthesizer.run(ctx);
        recordTraceAndTokens(ctx, AgentType.SYNTHESIZER.name(), output);
        log.info("Synthesizer phase complete. Recommendation: {}", output.result().recommendation());
        return output.result();
    }

    private void recordTraceAndTokens(AgentContext ctx, String key, AgentOutput<?> output) {
        ctx.state().putAgentTrace(key, output.result());
        ctx.state().addTokens(output.tokens());
    }

    private CompanyAnalysisResult createSuccessResult(CompanyAnalysisRequestEvent req, AgentContext ctx, SynthesizerAgentOutput finalOut, long startMs, ZonedDateTime now) {
        int tokens = ctx.state().getTotalTokens();
        log.info("Analysis workflow completed in {}ms. Tokens: {}", System.currentTimeMillis() - startMs, tokens);
        return CompanyAnalysisResult.builder()
                .correlationId(req.correlationId())
                .ticker(req.ticker())
                .market(req.market())
                .analysisDate(ctx.analysisDate())
                .status(AnalysisStatus.COMPLETED)
                .reportMd(finalOut.reportMd())
                .outlook(finalOut.outlook())
                .recommendation(finalOut.recommendation())
                .conviction(finalOut.conviction())
                .keyDrivers(finalOut.keyDrivers())
                .keyRisks(finalOut.keyRisks())
                .agentTrace(ctx.state().getAgentTrace())
                .tokenUsage(tokens)
                .createdAt(now)
                .updatedAt(ZonedDateTime.now())
                .build();
    }

    private CompanyAnalysisResult createFailureResult(CompanyAnalysisRequestEvent req, LocalDate analysisDate, Exception e, ZonedDateTime now) {
        return CompanyAnalysisResult.builder()
                .correlationId(req.correlationId())
                .ticker(req.ticker())
                .market(req.market())
                .analysisDate(analysisDate)
                .status(AnalysisStatus.FAILED)
                .error(e.getMessage())
                .createdAt(now)
                .updatedAt(ZonedDateTime.now())
                .build();
    }

    private Map<AgentType, StatementVariant> getAgentStatementVariants(CompanyAnalysisRequestEvent request, PlannerPlan plan) {
        Map<AgentType, StatementVariant> variants = new EnumMap<>(AgentType.class);
        if (request.statementVariant() != null) {
            Stream.of(AgentType.values()).filter(AgentType::isSpecialist).forEach(type -> variants.put(type, request.statementVariant()));
        } else if (plan != null) {
            Map<AgentType, Supplier<StatementVariant>> getters = Map.of(
                    AgentType.FUNDAMENTALS, plan::getFundamentalsVariant,
                    AgentType.CASH_FLOW, plan::getCashFlowVariant,
                    AgentType.VALUATION, plan::getValuationVariant,
                    AgentType.RISK, plan::getRiskVariant
            );
            getters.forEach((type, getter) -> {
                StatementVariant val = getter.get();
                if (val != null) {
                    variants.put(type, val);
                }
            });
        }
        return variants;
    }
}