package com.oraculum.analyst.agent.service.impl;

import com.oraculum.analyst.agent.dto.AgentContext;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.StandardAgentOutput;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.config.AnalystProperties;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.domain.TickerDocumentSubtype;
import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.TickerDocumentDto;
import com.oraculum.harvester.api.dto.CompanyNewsDto;
import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmMetrics;
import com.oraculum.llm.api.dto.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsAgentTest {

    @Mock
    private LlmRouterApi llmRouterApi;

    @Mock
    private PromptRegistry promptRegistry;

    private NewsAgent newsAgent;

    @BeforeEach
    void setUp() {
        AnalystProperties properties = new AnalystProperties(
                null, null, null, null,
                new AnalystProperties.News(30, 50, 5),
                null, 1000
        );
        newsAgent = new NewsAgent(llmRouterApi, promptRegistry, properties);
    }

    @Test
    void getName_returnsNews() {
        assertThat(newsAgent.getName()).isEqualTo(AgentType.NEWS);
    }

    @Test
    void run_whenNoNewsAndNoSecEx991_skipsLlmAndReturnsEmptyOutput() {
        CompanyDto company = createCompany("NVDA");
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .company(company)
                .recentNews(List.of())
                .build();

        AgentContext ctx = AgentContext.builder()
                .company(company)
                .analysisDate(LocalDate.of(2026, 10, 16))
                .factSheetData(factSheetData)
                .build();

        AgentOutput<StandardAgentOutput> output = newsAgent.run(ctx);

        assertThat(output).isNotNull();
        assertThat(output.tokens()).isEqualTo(0);
        assertThat(output.result().headline()).contains("No recent news articles or press releases found");
        verifyNoInteractions(llmRouterApi);
    }

    @Test
    void run_whenOnlySecEx991Present_executesLlmCall() {
        CompanyDto company = createCompany("NVDA");
        TickerDocumentDto secRelease = TickerDocumentDto.builder()
                .id("doc-1")
                .ticker("NVDA")
                .documentType(TickerDocumentType.SEC_8K)
                .documentSubtype(TickerDocumentSubtype.SEC_EX99_1)
                .filingDate(LocalDate.of(2026, 10, 10))
                .summary("{\"headline\":\"Q3 Earnings Release\"}")
                .build();
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .company(company)
                .recentNews(List.of())
                .recentSecDocuments(Map.of(
                        TickerDocumentType.SEC_8K, Map.of(TickerDocumentSubtype.SEC_EX99_1, List.of(secRelease))
                ))
                .build();

        when(promptRegistry.getPrompt(PromptType.NEWS))
                .thenReturn("News prompt for {{ ticker }}. News: {{ recent_news }}. SEC: {{ recent_sec_ex99_1_summaries }}.");

        StandardAgentOutput expectedOutput = StandardAgentOutput.builder()
                .headline("Q3 Earnings beat estimates")
                .score(7.5)
                .build();
        LlmResponse<StandardAgentOutput> llmResp = new LlmResponse<>(expectedOutput, new LlmMetrics(null, null, 100, 200, 300, 10L));
        when(llmRouterApi.executeCall(any(LlmCallRequest.class))).thenReturn(llmResp);

        AgentContext ctx = AgentContext.builder()
                .company(company)
                .analysisDate(LocalDate.of(2026, 10, 16))
                .correlationId(UUID.randomUUID())
                .factSheetData(factSheetData)
                .build();

        AgentOutput<StandardAgentOutput> output = newsAgent.run(ctx);

        assertThat(output).isNotNull();
        assertThat(output.tokens()).isEqualTo(300);
        verify(llmRouterApi).executeCall(any(LlmCallRequest.class));
    }

    @Test
    void run_whenNewsPresent_injectsRecencyTierAndWeightIntoPromptAndCallsLlm() {
        CompanyDto company = createCompany("NVDA");
        CompanyNewsDto newsDto = CompanyNewsDto.builder()
                .id(101L)
                .publishedAt(OffsetDateTime.of(2026, 10, 15, 14, 0, 0, 0, ZoneOffset.UTC))
                .headline("NVDA announces next-gen Blackwell architecture")
                .summary("Major AI GPU milestone")
                .related(List.of("NVDA"))
                .build();
        CompanyFactSheetData factSheetData = CompanyFactSheetData.builder()
                .company(company)
                .recentNews(List.of(newsDto))
                .build();

        when(promptRegistry.getPrompt(PromptType.NEWS))
                .thenReturn("News prompt for {{ ticker }} on {{ analysis_date }}. Focus: {{ analysis_focus }}. News: {{ recent_news }}. SEC: {{ recent_sec_ex99_1_summaries }}.");

        StandardAgentOutput expectedOutput = StandardAgentOutput.builder()
                .headline("Blackwell launch boosts sentiment")
                .score(8.0)
                .build();
        LlmResponse<StandardAgentOutput> llmResp = new LlmResponse<>(expectedOutput, new LlmMetrics(null, null, 100, 200, 300, 10L));
        when(llmRouterApi.executeCall(any(LlmCallRequest.class))).thenReturn(llmResp);

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

        ArgumentCaptor<LlmCallRequest> captor = ArgumentCaptor.forClass(LlmCallRequest.class);
        verify(llmRouterApi).executeCall(captor.capture());
        assertThat(captor.getValue().prompt())
                .contains("BREAKING")
                .contains("1.0");
    }

    private CompanyDto createCompany(String ticker) {
        return new CompanyDto(
                1, ticker, "US", ticker + " Inc", "1", "Tech", "Technology",
                "US123", "Desc", 1000L, "USD", "CIK1", "CUSIP1", null
        );
    }
}
