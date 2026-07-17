package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.NewsAgentOutput;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.company.api.dto.NewsTickerDto;
import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.CorrelationType;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class NewsAgent implements Agent<NewsAgentOutput> {

    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;

    @Override
    public AgentType getName() {
        return AgentType.NEWS;
    }

    @Override
    public AgentOutput<NewsAgentOutput> run(AgentContext ctx) {
        log.info("NewsAgent starting analysis for ticker: {}", ctx.ticker());
        String news = ctx.factSheetData().getRecentNews();
        String secEx991Summary = ctx.factSheetData().getRecentSecEx991Summaries();

        if (hasNoData(news) && hasNoData(secEx991Summary)) {
            log.warn("No recent news or 8-K documents found for ticker: {}", ctx.ticker());
            return new AgentOutput<>(new NewsAgentOutput("No significant recent news or material events found for this ticker."), 0);
        }

        String fullPrompt = buildFullPrompt(ctx, news, secEx991Summary);
        LlmResponse<NewsAgentOutput> response = llmRouterApi.executeCall(
                LlmCallRequest.of(LlmTierType.STANDARD, fullPrompt, NewsAgentOutput.class, ctx.correlationId(), CorrelationType.COMPANY_ANALYSIS, getName().name()));

        log.info("NewsAgent successfully generated summary for ticker: {}", ctx.ticker());
        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }

    private boolean hasNoData(String markdown) {
        return markdown == null || markdown.isBlank() || "[]".equals(markdown);
    }

    private String buildFullPrompt(AgentContext ctx, String news, String secEx991) {
        String relevanceDef = "No definition provided";
        String sentimentDef = "No definition provided";
        var newsList = ctx.factSheetData().getRecentNewsList();

        if (!newsList.isEmpty()) {
            NewsTickerDto firstNews = newsList.getFirst();
            relevanceDef = firstNews.relevanceScoreDefinition() != null ? firstNews.relevanceScoreDefinition() : relevanceDef;
            sentimentDef = firstNews.sentimentScoreDefinition() != null ? firstNews.sentimentScoreDefinition() : sentimentDef;
        }

        String systemPrompt = promptRegistry.getPrompt(PromptType.NEWS)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                .replace("{{ relevance_score_definition }}", relevanceDef)
                .replace("{{ sentiment_score_definition }}", sentimentDef)
                .replace("{{ news_sentiment_aggregate }}", ctx.factSheetData().getNewsSentimentAggregate())
                .replace("{{ recent_news }}", news)
                .replace("{{ recent_sec_ex99_1_summaries }}", secEx991)
                .replace("{{ ticker }}", ctx.ticker())
                .replace("{{ analysis_date }}", ctx.analysisDate().toString());

        return appendCriticFeedbackIfPresent(systemPrompt, ctx);
    }
}
