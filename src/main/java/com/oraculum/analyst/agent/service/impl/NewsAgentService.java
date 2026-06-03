package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.NewsAgentOutput;
import com.oraculum.analyst.agent.service.AgentDataTools;
import com.oraculum.analyst.agent.service.AgentService;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.AgentType;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsAgentService implements AgentService<NewsAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final AgentDataTools agentDataTools;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentType getName() {
        return AgentType.NEWS;
    }

    @Override
    public Class<NewsAgentOutput> getOutputModel() {
        return NewsAgentOutput.class;
    }

    @Override
    public AgentOutput<NewsAgentOutput> run(AgentContext ctx) {
        log.info("NewsAgent starting analysis for ticker: {}", ctx.ticker());

        String newsMarkdown = agentDataTools.getRecentNews(ctx.ticker(), 30, 100);

        if (newsMarkdown.contains("No recent news found")) {
            log.warn("No recent news found for ticker: {}", ctx.ticker());
            return new AgentOutput<>(new NewsAgentOutput("No significant recent news found for this ticker."), 0);
        }

        String systemPrompt = promptRegistry.getPrompt(PromptType.NEWS);
        String userPrompt = String.format("Here is the recent news for %s:\n\n%s", ctx.ticker(), newsMarkdown);
        String fullPrompt = systemPrompt + "\n" + userPrompt;

        LlmResponse<NewsAgentOutput> response = llmRouterApi.executeCall(LlmTierType.MINI,
                fullPrompt,
                NewsAgentOutput.class);

        log.info("NewsAgent successfully generated summary for ticker: {}", ctx.ticker());
        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}