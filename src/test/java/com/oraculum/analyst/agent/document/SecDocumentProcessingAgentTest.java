package com.oraculum.analyst.agent.document;

import com.oraculum.analyst.agent.document.dto.SecMdResponse;
import com.oraculum.analyst.agent.document.service.SecDocumentProcessingAgent;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.company.api.CompanyTickerDocumentApi;
import com.oraculum.company.api.domain.TickerDocumentProcessingStatus;
import com.oraculum.company.api.domain.TickerDocumentSubtype;
import com.oraculum.company.api.domain.TickerDocumentType;
import com.oraculum.company.api.dto.TickerDocumentDto;
import com.oraculum.company.api.dto.TickerDocumentPendingDto;
import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.LlmResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecDocumentProcessingAgentTest {

    @Mock
    private CompanyTickerDocumentApi companyTickerDocumentApi;

    @Mock
    private LlmRouterApi llmRouterApi;

    @Mock
    private PromptRegistry promptRegistry;

    @Mock
    private JsonMapper jsonMapper;

    @InjectMocks
    private SecDocumentProcessingAgent service;

    private TickerDocumentPendingDto rawDto;

    @BeforeEach
    void setUp() {
        rawDto = TickerDocumentPendingDto.builder()
                .id("hash123")
                .ticker("AAPL")
                .market("US")
                .documentType(TickerDocumentType.SEC_10K)
                .documentSubtype(TickerDocumentSubtype.SEC_MD)
                .reportPeriod(LocalDate.of(2023, 12, 31))
                .filingDate(LocalDate.of(2024, 2, 1))
                .content("MD&A Content here")
                .build();
    }

    @Test
    void processPendingDocuments_success() {
        // Arrange
        when(companyTickerDocumentApi.getPendingRawDocuments(1, 3)).thenReturn(List.of(rawDto));
        when(promptRegistry.getPrompt(PromptType.SEC_MD)).thenReturn("Review: {{ content }} for {{ ticker }}");

        SecMdResponse mdResponse = new SecMdResponse(
                "Good performance",
                List.of("Volume"),
                "Expanding",
                List.of("US +5%"),
                "NONE",
                "Positive",
                List.of("$50B Buyback"),
                Map.of("revenue_growth", 5.0),
                0.75
        );
        LlmResponse<SecMdResponse> response = new LlmResponse<>(mdResponse, null);
        when(llmRouterApi.executeCall(any(LlmCallRequest.class))).thenReturn(response);

        String expectedJson = "{\"summary\":\"Good performance\",\"sentiment_score\":0.75}";
        when(jsonMapper.writeValueAsString(mdResponse)).thenReturn(expectedJson);

        // Act
        service.processPendingDocuments(1, 3);

        ArgumentCaptor<TickerDocumentDto> summaryCaptor = ArgumentCaptor.forClass(TickerDocumentDto.class);
        verify(companyTickerDocumentApi).createDocumentSummary(summaryCaptor.capture());
        verify(companyTickerDocumentApi, never()).updateRawDocumentStatus(any(), any(), any());

        TickerDocumentDto saved = summaryCaptor.getValue();
        assertThat(saved.getId()).isEqualTo("hash123");
        assertThat(saved.getSentimentScore()).isEqualTo(0.75f);
        assertThat(saved.getSummary()).isEqualTo(expectedJson);
        assertThat(saved.getDocumentType()).isEqualTo(TickerDocumentType.SEC_10K);
        assertThat(saved.getDocumentSubtype()).isEqualTo(TickerDocumentSubtype.SEC_MD);
    }

    @Test
    void processPendingDocuments_llmFailure_marksAsFailed() {
        // Arrange
        when(companyTickerDocumentApi.getPendingRawDocuments(1, 3)).thenReturn(List.of(rawDto));
        when(promptRegistry.getPrompt(PromptType.SEC_MD)).thenReturn("Review: {{ content }}");
        when(llmRouterApi.executeCall(any(LlmCallRequest.class))).thenThrow(new RuntimeException("LLM down"));

        // Act
        service.processPendingDocuments(1, 3);

        // Assert
        verify(companyTickerDocumentApi).updateRawDocumentStatus("hash123", LocalDate.of(2023, 12, 31), TickerDocumentProcessingStatus.FAILED);
        verify(companyTickerDocumentApi, never()).createDocumentSummary(any());
    }
}
