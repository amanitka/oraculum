package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.NewsAgentOutput;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.company.api.dto.NewsTickerDto;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmResponse;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

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
        String newsMarkdown = ctx.factSheetData().getRecentNews();
        List<NewsTickerDto> newsList = ctx.factSheetData().getRecentNewsList();

        if (newsMarkdown == null || "[]".equals(newsMarkdown) || newsMarkdown.isBlank() || newsList == null || newsList.isEmpty()) {
            log.warn("No recent news found for ticker: {}", ctx.ticker());
            return new AgentOutput<>(new NewsAgentOutput("No significant recent news found for this ticker."), 0);
        }

        // Extract definitions from the first news item dynamically to prevent hardcoding
        NewsTickerDto firstNews = newsList.getFirst();
        String relevanceDef = firstNews.relevanceScoreDefinition() != null ? firstNews.relevanceScoreDefinition() : "No definition " +
                                                                                                                    "provided";
        String sentimentDef = firstNews.sentimentScoreDefinition() != null ? firstNews.sentimentScoreDefinition() : "No definition " +
                                                                                                                    "provided";

        String systemPrompt = promptRegistry.getPrompt(PromptType.NEWS)
                .replace("{{ analysis_focus }}", ctx.analysisFocus() != null ? ctx.analysisFocus() : "Standard comprehensive analysis.")
                .replace("{{ relevance_score_definition }}", relevanceDef)
                .replace("{{ sentiment_score_definition }}", sentimentDef)
                .replace("{{ news_sentiment_aggregate }}", ctx.factSheetData().getNewsSentimentAggregate())
                .replace("{{ recent_news }}", newsMarkdown);

        String userPrompt = String.format("Analyze the recent news and sentiment for %s as of %s based on the provided data.",
                ctx.ticker(),
                ctx.analysisDate());
        String fullPrompt = appendCriticFeedbackIfPresent(systemPrompt + "\n" + userPrompt, ctx);

        LlmResponse<NewsAgentOutput> response = llmRouterApi.executeCall(LlmTierType.STANDARD, fullPrompt, NewsAgentOutput.class);

        log.info("NewsAgent successfully generated summary for ticker: {}", ctx.ticker());
        return new AgentOutput<>(response.result(), response.metrics().totalTokens());
    }
}
