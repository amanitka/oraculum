package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.StandardAgentOutput;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.NewsTickerDto;
import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmMetrics;
import com.oraculum.llm.api.dto.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsAgentTest {

    @Mock
    private LlmRouterApi llmRouterApi;

    @Mock
    private PromptRegistry promptRegistry;

    @Mock
    private CompanyFactSheetData factSheetData;

    private NewsAgent newsAgent;

    @BeforeEach
    void setUp() {
        newsAgent = new NewsAgent(llmRouterApi, promptRegistry);
    }

    @Test
    void getName_returnsNews() {
        assertThat(newsAgent.getName()).isEqualTo(AgentType.NEWS);
    }

    @Test
    void run_whenNoNewsInFactSheet_skipsLlmAndReturnsEmptyOutput() {
        when(factSheetData.getRecentNews()).thenReturn("[]");

        CompanyDto company = mock(CompanyDto.class);
        when(company.ticker()).thenReturn("NVDA");

        AgentContext ctx = AgentContext.builder()
                .company(company)
                .factSheetData(factSheetData)
                .build();

        AgentOutput<StandardAgentOutput> output = newsAgent.run(ctx);

        assertThat(output).isNotNull();
        assertThat(output.tokens()).isEqualTo(0);
        assertThat(output.result().headline()).contains("No recent news articles found");
        verifyNoInteractions(llmRouterApi);
    }

    @Test
    void run_whenNewsPresent_executesLlmCall() {
        when(factSheetData.getRecentNews()).thenReturn("[{\"title\":\"Nvidia releases Blackwell\"}]");
        when(factSheetData.getRecentSecEx991Summaries()).thenReturn("[]");
        when(factSheetData.getRecentNewsList()).thenReturn(List.of(mock(NewsTickerDto.class)));
        when(factSheetData.getNewsSentimentAggregate()).thenReturn("{}");

        when(promptRegistry.getPrompt(PromptType.NEWS)).thenReturn("News prompt for {{ ticker }} on {{ analysis_date }}. Focus: {{ analysis_focus }}. Relevance: {{ relevance_score_definition }}. Sentiment: {{ sentiment_score_definition }}. Aggregate: {{ news_sentiment_aggregate }}. News: {{ recent_news }}. SEC: {{ recent_sec_ex99_1_summaries }}.");

        StandardAgentOutput expectedOutput = StandardAgentOutput.builder()
                .headline("Blackwell launch boosts sentiment")
                .score(8.0)
                .build();

        LlmResponse<StandardAgentOutput> llmResp = new LlmResponse<>(expectedOutput, new LlmMetrics(null, null, 100, 200, 300, 10L));
        when(llmRouterApi.executeCall(any(LlmCallRequest.class))).thenReturn(llmResp);

        CompanyDto company = mock(CompanyDto.class);
        when(company.ticker()).thenReturn("NVDA");

        AgentContext ctx = AgentContext.builder()
                .company(company)
                .analysisDate(LocalDate.of(2026, 10, 16))
                .correlationId(UUID.randomUUID())
                .factSheetData(factSheetData)
                .build();

        AgentOutput<StandardAgentOutput> output = newsAgent.run(ctx);

        assertThat(output).isNotNull();
        assertThat(output.tokens()).isEqualTo(300);
        assertThat(output.result().headline()).isEqualTo("Blackwell launch boosts sentiment");
        verify(llmRouterApi).executeCall(any(LlmCallRequest.class));
    }
}
