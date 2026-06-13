package com.oraculum.analyst.service;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.PlannerPlan;
import com.oraculum.analyst.agent.dto.SynthesizerAgentOutput;
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
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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

    private Map<AgentType, StatementVariant> getAgentStatementVariants(CompanyAnalysisRequestEvent request, PlannerPlan plan) {
        Map<AgentType, StatementVariant> variants = new EnumMap<>(AgentType.class);
        if (request.statementVariant() != null) {
            Stream.of(AgentType.values()).filter(AgentType::isSpecialist).forEach(type -> variants.put(type, request.statementVariant()));
        } else if (plan != null) {
            Map<AgentType, Supplier<StatementVariant>> getters = Map.of(AgentType.FUNDAMENTALS,
                    plan::getFundamentalsVariant,
                    AgentType.CASH_FLOW,
                    plan::getCashFlowVariant,
                    AgentType.VALUATION,
                    plan::getValuationVariant,
                    AgentType.RISK,
                    plan::getRiskVariant);
            getters.forEach((type, getter) -> {
                StatementVariant val = getter.get();
                if (val != null)
                    variants.put(type, val);
            });
        }
        return variants;
    }

    public CompanyAnalysisResult run(CompanyAnalysisRequestEvent request) {
        long startTime = System.currentTimeMillis();
        int totalTokens = 0;
        Map<AgentType, Object> agentTrace = new EnumMap<>(AgentType.class);
        ZonedDateTime now = ZonedDateTime.now();

        log.info("Starting analysis workflow for ticker {}", request.ticker());
        CompanyDto company = companyApi.getCompanyById(request.companyId());
        if (company == null) {
            throw new IllegalArgumentException("Company not found for ticker: " + request.ticker());
        }

        CompanyFactSheetData factSheetData = companyFactSheetDataService.create(company);

        AgentContext initialCtx = new AgentContext(company,
                factSheetData,
                request.analysisDate() != null ? request.analysisDate() : LocalDate.now(),
                request.statementVariant(),
                null,
                analystProperties.tokenBudget(),
                null,
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
                    factSheetData,
                    initialCtx.analysisDate(),
                    request.statementVariant(),
                    getAgentStatementVariants(request, plan),
                    analystProperties.tokenBudget(),
                    plan.getAnalysisFocus(),
                    new EnumMap<>(AgentType.class));

            List<Agent<?>> specialists = Arrays.stream(AgentType.values())
                    .filter(AgentType::isSpecialist)
                    .sorted(java.util.Comparator.comparingInt(AgentType::getExecutionOrder))
                    .map(agents::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            for (Agent<?> agent : specialists) {
                log.info("Starting {} phase", agent.getName());
                var output = agent.run(sharedCtx);
                sharedCtx.agentOutputs().put(agent.getName(), output.result());
                totalTokens += output.tokens();
                agentTrace.put(agent.getName(), output.result());
                log.info("{} phase complete. Tokens: {}", agent.getName(), output.tokens());
            }

            log.info("Starting Critic phase");
            Agent<?> critic = agents.get(AgentType.CRITIC);
            var criticOutput = critic.run(sharedCtx);
            sharedCtx.agentOutputs().put(AgentType.CRITIC, criticOutput.result());
            totalTokens += criticOutput.tokens();
            agentTrace.put(AgentType.CRITIC, criticOutput.result());
            log.info("Critic phase complete. Tokens: {}. Consistent: {}",
                    criticOutput.tokens(),
                    ((com.oraculum.analyst.agent.dto.CriticAgentOutput) criticOutput.result()).isConsistent());

            log.info("Starting Synthesizer phase");
            Agent<SynthesizerAgentOutput> synthesizer = (Agent<SynthesizerAgentOutput>) agents.get(AgentType.SYNTHESIZER);
            var finalOutput = synthesizer.run(sharedCtx);
            totalTokens += finalOutput.tokens();
            agentTrace.put(AgentType.SYNTHESIZER, finalOutput.result());
            log.info("Synthesizer phase complete. Tokens: {}. Recommendation: {}",
                    finalOutput.tokens(),
                    finalOutput.result().recommendation());

            long elapsedMs = System.currentTimeMillis() - startTime;
            log.info("Analysis workflow completed successfully in {}ms. Total tokens: {}", elapsedMs, totalTokens);

            return new CompanyAnalysisResult(request.correlationId(),
                    request.ticker(),
                    request.market(),
                    sharedCtx.analysisDate(),
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
            return new CompanyAnalysisResult(request.correlationId(),
                    request.ticker(),
                    request.market(),
                    initialCtx.analysisDate(),
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