# Implementation Plan: Advanced Agent Consistency Strategies (Strategies 2 & 3)

This guide provides step-by-step instructions to implement **Strategy 2 (Pre-computed Structured Signals)** and **Strategy 3 (Critique-and-Refine Loop)** within the Oraculum codebase to further minimize logic conflicts between specialist agents.

---

## 📊 Strategy 2: Pre-computed Structured Signals (Data-Level)

### Objective
Instead of forcing LLM agents to parse and interpret raw time-series arrays of daily prices (which is token-heavy and mathematically error-prone), we compute technical momentum signals in Java deterministically and pass them to the agents as clean metadata.

### Step 1: Create a Technical Indicators DTO
Define a new DTO `TechnicalIndicatorsDto` in `com.oraculum.analyst.dto` to capture pre-calculated price statistics:

```java
package com.oraculum.analyst.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TechnicalIndicatorsDto(
    @JsonProperty("current_price") double currentPrice,
    @JsonProperty("current_vs_50d_sma_pct") double currentVs50dSmaPct,
    @JsonProperty("current_vs_200d_sma_pct") double currentVs200dSmaPct,
    @JsonProperty("is_currently_above_50d_sma") boolean isCurrentlyAbove50dSma,
    @JsonProperty("is_currently_above_200d_sma") boolean isCurrentlyAbove200dSma,
    @JsonProperty("lowest_vs_50d_sma_last_30d_pct") double lowestVs50dSmaLast30dPct,
    @JsonProperty("did_cross_below_50d_sma_last_30d") boolean didCrossBelow50dSmaLast30d,
    @JsonProperty("volume_velocity_average_30d") double volumeVelocityAverage30d
) {}
```

### Step 2: Implement Calculation Logic in `CompanyFactSheetData`
Update [CompanyFactSheetData.java](file:///G:/Git/oraculum/src/main/java/com/oraculum/analyst/dto/CompanyFactSheetData.java) to calculate these values from `dailySharePriceSignals`:

```java
public TechnicalIndicatorsDto getTechnicalIndicators() {
    if (dailySharePriceSignals == null || dailySharePriceSignals.isEmpty()) {
        return null;
    }
    
    // 1. Get current (latest) signals
    var latest = dailySharePriceSignals.get(dailySharePriceSignals.size() - 1);
    double currentPrice = latest.closePrice(); // or current equivalent field
    double currentVs50 = latest.pctFrom50dSma();
    double currentVs200 = latest.pctFrom200dSma();
    
    // 2. Scan past 30 days for lowest points and crossings
    double lowestVs50 = Double.MAX_VALUE;
    boolean didCrossBelow50 = false;
    double volSum = 0;
    
    for (var sig : dailySharePriceSignals) {
        if (sig.pctFrom50dSma() < lowestVs50) {
            lowestVs50 = sig.pctFrom50dSma();
        }
        if (sig.pctFrom50dSma() < 0) {
            didCrossBelow50 = true;
        }
        volSum += sig.volumeVelocity();
    }
    
    return new TechnicalIndicatorsDto(
        currentPrice,
        currentVs50,
        currentVs200,
        currentVs50 > 0,
        currentVs200 > 0,
        lowestVs50,
        didCrossBelow50,
        volSum / dailySharePriceSignals.size()
    );
}
```

### Step 3: Inject the DTO into Agent Prompts
Expose the JSON representation in `CompanyFactSheetData`:
```java
public String getTechnicalIndicatorsJson() {
    return JsonUtils.toJson(objectMapper, getTechnicalIndicators(), "{}");
}
```
Replace raw `daily_share_price_signals` inside `RiskAgent` and `SharePriceAgent` with `{{ technical_indicators }}`.

---

## 🔄 Strategy 3: Critique-and-Refine Loop (Workflow-Level)

### Objective
Introduce a multi-turn critique loop inside [CompanyAnalysisWorkflowService.java](file:///G:/Git/oraculum/src/main/java/com/oraculum/analyst/service/CompanyAnalysisWorkflowService.java) where agents are forced to reconcile their statements before the Synthesizer runs.

### Step 1: Create a Refinable Interface or Method in `Agent`
Allow agents to receive the Critic's feedback and their previous output to refine their analysis:

```java
// Inside com.oraculum.analyst.agent.service.Agent.java
default AgentOutput<T> refine(AgentContext ctx, T previousOutput, String criticFeedback) {
    // Default implementation falls back to run if refinement is not supported
    return run(ctx);
}
```

### Step 2: Implement Refinement Logic in Specialists (e.g., `RiskAgent`)
Implement the refinement method in `RiskAgent` (and other specialists):

```java
@Override
public AgentOutput<RiskAgentOutput> refine(AgentContext ctx, RiskAgentOutput previousOutput, String criticFeedback) {
    CompanyFactSheetData factSheet = ctx.factSheetData();
    StatementVariant variant = ctx.getVariantFor(getName());

    String basePrompt = promptRegistry.getPrompt(PromptType.RISK);
    
    // Append refinement instruction to the prompt
    String refinementInstruction = String.format(
        "\n\n### REFINEMENT INSTRUCTIONS:\n" +
        "You previously analyzed the company and outputted: %s\n" +
        "The Critic Agent identified the following contradiction in the team's analysis: \n" +
        "\"%s\"\n" +
        "Re-evaluate your findings against this criticism and the raw data. " +
        "Reconcile your statement so it aligns logically with the other agents, or justify why both facts are correct.",
        JsonUtils.toJson(objectMapper, previousOutput, "{}"),
        criticFeedback
    );

    String fullPrompt = basePrompt + refinementInstruction;

    LlmResponse<RiskAgentOutput> response = llmRouterApi.executeCall(LlmTierType.MINI,
            fullPrompt,
            RiskAgentOutput.class);

    return new AgentOutput<>(response.result(), response.metrics().totalTokens());
}
```

### Step 3: Implement the Workflow Loop in `CompanyAnalysisWorkflowService`
Modify the specialist orchestration loop to allow up to `MAX_RETRIES` refinement iterations:

```java
int maxRetries = 2;
boolean consistent = false;
CriticAgentOutput criticOutput = null;

for (int attempt = 0; attempt < maxRetries; attempt++) {
    // 1. Run specialists
    for (Agent<?> agent : specialists) {
        if (attempt == 0) {
            var output = agent.run(sharedCtx);
            sharedCtx.priorOutputs().put(agent.getName(), output.result());
            totalTokens += output.tokens();
        } else if (!consistent && criticOutput != null) {
            // Only refine if contradictions were found
            String feedback = String.join("; ", criticOutput.contradictionsFound());
            // Safe call to refinement
            var output = refineAgentHelper(agent, sharedCtx, feedback);
            sharedCtx.priorOutputs().put(agent.getName(), output.result());
            totalTokens += output.tokens();
        }
    }
    
    // 2. Run Critic
    log.info("Starting Critic phase (Attempt {})", attempt + 1);
    Agent<?> critic = agents.get(AgentType.CRITIC);
    var criticResult = critic.run(sharedCtx);
    criticOutput = (CriticAgentOutput) criticResult.result();
    sharedCtx.priorOutputs().put(AgentType.CRITIC, criticOutput);
    totalTokens += criticResult.tokens();
    
    consistent = criticOutput.isConsistent();
    if (consistent) {
        log.info("Analysis workflow is consistent after {} attempts", attempt + 1);
        break;
    }
}
```

Helper method to safely call refinement using generics:
```java
@SuppressWarnings("unchecked")
private <T> AgentOutput<T> refineAgentHelper(Agent<T> agent, AgentContext ctx, String feedback) {
    T prev = (T) ctx.priorOutputs().get(agent.getName());
    return agent.refine(ctx, prev, feedback);
}
```
