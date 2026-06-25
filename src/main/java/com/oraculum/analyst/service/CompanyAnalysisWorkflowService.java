package com.oraculum.analyst.service;

import com.oraculum.analyst.agent.dto.*;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.api.domain.AnalysisStatus;
import com.oraculum.analyst.api.dto.CompanyAnalysisRequestEvent;
import com.oraculum.analyst.api.event.CompanyAnalysisProgressEvent;
import com.oraculum.analyst.config.AnalystProperties;
import com.oraculum.analyst.dto.CompanyAnalysisResult;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.company.api.dto.CompanyDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyAnalysisWorkflowService {

    private final CompanyMetadataApi companyMetadataApi;
    private final AnalystProperties analystProperties;
    private final CompanyFactSheetDataService companyFactSheetDataService;
    private final Map<AgentType, Agent<?>> agents;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public CompanyAnalysisResult run(CompanyAnalysisRequestEvent request) {
        long startMs = System.currentTimeMillis();
        ZonedDateTime now = ZonedDateTime.now();

        log.info("Starting analysis workflow for ticker {}", request.ticker());
        try {
            AgentContext ctx = initializeContext(request);

            runSpecialistPhase(request, ctx);
            runCriticCorrectionLoop(request, ctx);
            SynthesizerAgentOutput finalOutput = runSynthesizerPhase(request, ctx);

            return createSuccessResult(request, ctx, finalOutput, startMs, now);
        } catch (Exception e) {
            log.error("Workflow failed after {}ms: {}", System.currentTimeMillis() - startMs, e.getMessage(), e);
            LocalDate analysisDate = request.analysisDate() != null ? request.analysisDate() : LocalDate.now();
            return createFailureResult(request, analysisDate, e, now); // Can't easily recover partial state here unless we pass it up
        }
    }

    private AgentContext initializeContext(CompanyAnalysisRequestEvent request) {
        CompanyDto company = companyMetadataApi.getCompanyById(request.companyId());
        if (company == null) {
            throw new IllegalArgumentException("Company not found for ticker: " + request.ticker());
        }

        AgentWorkflowState state = new AgentWorkflowState();
        String focus = request.analysisFocus();
        if (focus == null || focus.isBlank()) {
            focus = "Standard comprehensive fundamental, valuation, and risk analysis.";
        }
        state.setAnalysisFocus(focus);

        CompanyFactSheetData factSheetData = companyFactSheetDataService.create(company, state.getCitationRegistry());
        LocalDate analysisDate = request.analysisDate() != null ? request.analysisDate() : LocalDate.now();

        return new AgentContext(request.correlationId(), company, factSheetData, analysisDate, analystProperties.tokenBudget(), state);
    }


    private void runSpecialistPhase(CompanyAnalysisRequestEvent request, AgentContext ctx) {
        List<Agent<?>> specialists = Arrays.stream(AgentType.values())
                .filter(AgentType::isSpecialist)
                .sorted(Comparator.comparingInt(AgentType::getExecutionOrder))
                .map(agents::get)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        for (Agent<?> agent : specialists) {
            log.info("Starting {} phase", agent.getName());
            eventPublisher.publishEvent(new CompanyAnalysisProgressEvent(request.correlationId(), agent.getName(), false));
            var output = agent.run(ctx);
            ctx.state().putAgentOutput(agent.getName(), output.result());
            recordTraceAndTokens(ctx, agent.getName().name(), output);
            log.info("{} phase complete. Tokens: {}", agent.getName(), output.tokens());
        }
    }

    private void runCriticCorrectionLoop(CompanyAnalysisRequestEvent request, AgentContext ctx) {
        int maxReruns = analystProperties.critic().maxReruns();
        int maxSpecialistsPerRerun = analystProperties.critic().maxSpecialistsPerRerun();
        Agent<?> critic = agents.get(AgentType.CRITIC);

        for (int rerunCount = 0; rerunCount <= maxReruns; rerunCount++) {
            log.info("Starting Critic phase (rerunCount: {})", rerunCount);
            eventPublisher.publishEvent(new CompanyAnalysisProgressEvent(request.correlationId(), AgentType.CRITIC, false));
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

            executeSpecialistReruns(request, ctx, toRerun, rerunCount);
        }
    }

    private List<CriticAgentOutput.RerunInstruction> getTopReruns(CriticAgentOutput output, int limit) {
        if (output.recommendedReruns() == null) {
            return List.of();
        }
        return output.recommendedReruns().stream()
                .filter(r -> r.specialist().isSupportsRerun())
                .sorted(Comparator.comparingInt(CriticAgentOutput.RerunInstruction::severity))
                .limit(limit)
                .sorted(Comparator.comparingInt(r -> r.specialist().getExecutionOrder()))
                .toList();
    }

    private void executeSpecialistReruns(CompanyAnalysisRequestEvent request, AgentContext ctx, List<CriticAgentOutput.RerunInstruction> reruns, int rerunCount) {
        log.info("Critic triggered rerun {} for: {}", rerunCount + 1, reruns.stream().map(CriticAgentOutput.RerunInstruction::specialist).toList());

        Map<AgentType, String> feedbackMap = reruns.stream()
                .collect(Collectors.toMap(CriticAgentOutput.RerunInstruction::specialist, CriticAgentOutput.RerunInstruction::instruction));
        ctx.state().setCriticFeedback(feedbackMap);

        for (CriticAgentOutput.RerunInstruction instruction : reruns) {
            AgentType type = instruction.specialist();
            Agent<?> agent = agents.get(type);
            log.info("Re-running {} phase based on Critic feedback", type.getAgentName());
            eventPublisher.publishEvent(new CompanyAnalysisProgressEvent(request.correlationId(), type, false));
            var output = agent.run(ctx);
            ctx.state().putAgentOutput(type, output.result());
            recordTraceAndTokens(ctx, type.name() + "_RERUN_" + rerunCount, output);
        }
        ctx.state().clearCriticFeedback();
    }

    private SynthesizerAgentOutput runSynthesizerPhase(CompanyAnalysisRequestEvent request, AgentContext ctx) {
        log.info("Starting Synthesizer phase");
        eventPublisher.publishEvent(new CompanyAnalysisProgressEvent(request.correlationId(), AgentType.SYNTHESIZER, false));
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

        injectPrunedCitationsToTrace(ctx);

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

    private void injectPrunedCitationsToTrace(AgentContext ctx) {
        String fullTraceStr = objectMapper.writeValueAsString(ctx.state().getAgentTrace());
        Map<Integer, Object> allCitations = ctx.state().getCitationRegistry().getCitations();
        Map<String, Object> prunedCitations = new java.util.HashMap<>();

        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\[(\\d+)\\]");
        java.util.regex.Matcher m = p.matcher(fullTraceStr);
        while (m.find()) {
            String idStr = m.group(1);
            try {
                int id = Integer.parseInt(idStr);
                if (allCitations.containsKey(id)) {
                    prunedCitations.put(idStr, allCitations.get(id));
                }
            } catch (NumberFormatException ignored) {
            }
        }
        ctx.state().putAgentTrace("_citations", prunedCitations);
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

}