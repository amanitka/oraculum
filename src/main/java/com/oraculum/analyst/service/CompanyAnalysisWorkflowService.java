package com.oraculum.analyst.service;

import com.oraculum.analyst.agents.base.Agent;
import com.oraculum.analyst.agents.context.AgentContext;
import com.oraculum.analyst.agents.models.PlannerPlan;
import com.oraculum.analyst.agents.models.SynthesizerAgentOutput;
import com.oraculum.analyst.agents.tools.DataTools;
import com.oraculum.analyst.config.AnalystProperties;
import com.oraculum.analyst.domain.AgentType;
import com.oraculum.analyst.domain.AnalysisStatus;
import com.oraculum.analyst.dto.CompanyAnalysisRequest;
import com.oraculum.analyst.dto.CompanyAnalysisResultDto;
import com.oraculum.company.api.dto.CompanyDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyAnalysisWorkflowService {

    private final DataTools dataTools;
    private final AnalystProperties analystProperties;
    private final Map<AgentType, Agent<?>> agents;

    public CompanyAnalysisResultDto run(CompanyAnalysisRequest request, UUID correlationId) {
        long startTime = System.currentTimeMillis();
        int totalTokens = 0;
        Map<AgentType, Object> agentTrace = new EnumMap<>(AgentType.class);
        ZonedDateTime now = ZonedDateTime.now();

        log.info("Starting analysis workflow for ticker {}", request.ticker());
        CompanyDto company = dataTools.getCompany(request.ticker(), request.market());
        if (company == null) {
            throw new IllegalArgumentException("Company not found for ticker: " + request.ticker());
        }

        AgentContext initialCtx = new AgentContext(company,
                request.requestDate() != null ? request.requestDate() : LocalDate.now(),
                request.defaultVariant(),
                analystProperties.tokenBudget(),
                new EnumMap<>(AgentType.class));

        try {
            log.info("Starting Planner phase");
            Agent<PlannerPlan> planner = (Agent<PlannerPlan>) agents.get(AgentType.PLANNER);
            var planOut = planner.run(initialCtx);
            PlannerPlan plan = planOut.result();
            totalTokens += planOut.tokens();
            agentTrace.put(AgentType.PLANNER, plan);
            log.info("Planner phase complete. Tokens: {}. Plan: {}", planOut.tokens(), plan);

            AgentContext sharedCtx = new AgentContext(company,
                    initialCtx.requestDate(),
                    request.defaultVariant(),
                    analystProperties.tokenBudget(),
                    new EnumMap<>(AgentType.class));

            log.info("Starting FactSheet phase");
            Agent<?> factSheetAgent = agents.get(AgentType.FACT_SHEET);
            var factSheetOut = factSheetAgent.run(sharedCtx);
            totalTokens += factSheetOut.tokens();
            sharedCtx.priorOutputs().put(AgentType.FACT_SHEET, factSheetOut.result());
            log.info("FactSheet phase complete.");

            List<Agent<?>> specialists = java.util.Arrays.stream(AgentType.values())
                    .filter(AgentType::isSpecialist)
                    .map(agents::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            for (Agent<?> agent : specialists) {
                log.info("Starting {} phase", agent.getName());
                var output = agent.run(sharedCtx);
                sharedCtx.priorOutputs().put(agent.getName(), output.result());
                totalTokens += output.tokens();
                agentTrace.put(agent.getName(), output.result());
                log.info("{} phase complete. Tokens: {}", agent.getName(), output.tokens());
            }

            log.info("Starting Critic phase");
            Agent<?> critic = agents.get(AgentType.CRITIC);
            var criticOutput = critic.run(sharedCtx);
            sharedCtx.priorOutputs().put(AgentType.CRITIC, criticOutput.result());
            totalTokens += criticOutput.tokens();
            agentTrace.put(AgentType.CRITIC, criticOutput.result());
            log.info("Critic phase complete. Tokens: {}. Consistent: {}",
                    criticOutput.tokens(),
                    ((com.oraculum.analyst.agents.models.CriticAgentOutput) criticOutput.result()).isConsistent());

            log.info("Starting Synthesizer phase");
            Agent<SynthesizerAgentOutput> synthesizer =
                    (Agent<SynthesizerAgentOutput>) agents.get(AgentType.SYNTHESIZER);
            var finalOutput = synthesizer.run(sharedCtx);
            totalTokens += finalOutput.tokens();
            agentTrace.put(AgentType.SYNTHESIZER, finalOutput.result());
            log.info("Synthesizer phase complete. Tokens: {}. Recommendation: {}",
                    finalOutput.tokens(),
                    finalOutput.result().recommendation());

            long elapsedMs = System.currentTimeMillis() - startTime;
            log.info("Analysis workflow completed successfully in {}ms. Total tokens: {}", elapsedMs, totalTokens);

            return new CompanyAnalysisResultDto(correlationId,
                    request.ticker(),
                    request.market(),
                    sharedCtx.requestDate(),
                    AnalysisStatus.COMPLETED,
                    finalOutput.result().reportMd(),
                    finalOutput.result().outlook(),
                    finalOutput.result().recommendation(),
                    finalOutput.result().conviction(),
                    finalOutput.result().keyDrivers(),
                    finalOutput.result().keyRisks(),
                    agentTrace,
                    totalTokens,
                    null,
                    now,
                    ZonedDateTime.now());
        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - startTime;
            log.error("Workflow failed after {}ms: {}", elapsedMs, e.getMessage(), e);
            return new CompanyAnalysisResultDto(correlationId,
                    request.ticker(),
                    request.market(),
                    initialCtx.requestDate(),
                    AnalysisStatus.FAILED,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    agentTrace,
                    totalTokens,
                    e.getMessage(),
                    now,
                    ZonedDateTime.now());
        }
    }
}