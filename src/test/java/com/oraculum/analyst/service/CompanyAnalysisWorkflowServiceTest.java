package com.oraculum.analyst.service;

import com.oraculum.analyst.agent.document.service.SecDocumentProcessingAgent;
import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.CriticAgentOutput;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.api.domain.AnalysisOutlook;
import com.oraculum.analyst.api.domain.ValuationAssessment;
import com.oraculum.analyst.api.dto.AnalysisResult;
import com.oraculum.analyst.api.dto.CompanyAnalysisRequestEvent;
import com.oraculum.analyst.api.event.CompanyAnalysisProgressEvent;
import com.oraculum.analyst.config.AnalystProperties;
import com.oraculum.analyst.dto.CompanyAnalysisResult;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.analyst.service.citation.CitationManagementService;
import com.oraculum.analyst.service.citation.CitationVerifierService;
import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.company.api.dto.CompanyDto;
import com.oraculum.company.api.dto.TickerKeyDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompanyAnalysisWorkflowServiceTest {

    @Mock
    private CompanyMetadataApi companyMetadataApi;

    @Mock
    private CompanyFactSheetDataService companyFactSheetDataService;

    @Mock
    private SecDocumentProcessingAgent secDocumentProcessingAgent;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CitationVerifierService citationVerifierService;

    @Mock
    private CitationManagementService citationManagementService;

    @Mock
    private DataQualityMetricsCalculator dataQualityMetricsCalculator;

    @Mock
    private SynthesizerGuardrailService synthesizerGuardrailService;

    private Map<AgentType, Agent<?>> agents;
    private CompanyAnalysisWorkflowService workflowService;
    private CompanyAnalysisRequestEvent request;

    @BeforeEach
    void setUp() {
        agents = new EnumMap<>(AgentType.class);
        AnalystProperties analystProperties = new AnalystProperties(
                null, null, null, null, null,
                new AnalystProperties.Critic(3, 2),
                100000
        );
        workflowService = new CompanyAnalysisWorkflowService(
                companyMetadataApi,
                analystProperties,
                companyFactSheetDataService,
                secDocumentProcessingAgent,
                agents,
                eventPublisher,
                citationVerifierService,
                citationManagementService,
                dataQualityMetricsCalculator,
                synthesizerGuardrailService
        );

        when(citationVerifierService.verifyRecordCitations(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(synthesizerGuardrailService.applyGuardrails(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(citationManagementService.formatReportWithFootnote(any())).thenAnswer(inv -> inv.getArgument(0));

        request = new CompanyAnalysisRequestEvent(
                UUID.randomUUID(),
                1,
                new TickerKeyDto("AAPL", "US"),
                LocalDate.now(),
                "test focus",
                null
        );
    }

    @Test
    void run_withMissingCompany_failsFast() {
        when(companyMetadataApi.getCompanyById(1)).thenReturn(null);

        CompanyAnalysisResult result = workflowService.run(request);

        assertNotNull(result);
        assertNotNull(result.error());
    }

    @Test
    void run_successfulAnalysis_noReruns() {
        CompanyDto companyDto = new CompanyDto(
                1, "AAPL", "US", "Apple Inc", "TECH", "Consumer Electronics", "Technology",
                "US0378331005", "Desc", 100000L, "USD", "0000320193", "037833100", null
        );
        when(companyMetadataApi.getCompanyById(1)).thenReturn(companyDto);
        when(companyFactSheetDataService.create(eq(companyDto), any()))
                .thenReturn(CompanyFactSheetData.builder().company(companyDto).build());

        Agent<CriticAgentOutput> criticAgent = mock(Agent.class);
        agents.put(AgentType.CRITIC, criticAgent);
        AgentOutput<CriticAgentOutput> criticOutput = new AgentOutput<>(
                new CriticAgentOutput(List.of(), true, null), 100
        );
        when(criticAgent.run(any())).thenReturn(criticOutput);

        Agent<AnalysisResult> synthesizerAgent = mock(Agent.class);
        agents.put(AgentType.SYNTHESIZER, synthesizerAgent);
        AnalysisResult expectedAnalysisResult = new AnalysisResult(
                "Report", "Reasoning", Map.of(),
                AnalysisOutlook.BULLISH, ValuationAssessment.UNDERVALUED, 4,
                "Thesis", List.of("Bull"), List.of("Bear"), "Valuation", "Watch"
        );
        AgentOutput<AnalysisResult> synthOutput = new AgentOutput<>(expectedAnalysisResult, 200);
        when(synthesizerAgent.run(any())).thenReturn(synthOutput);

        CompanyAnalysisResult result = workflowService.run(request);

        assertNotNull(result);
        assertNull(result.error());
        verify(eventPublisher, atLeastOnce()).publishEvent(any(CompanyAnalysisProgressEvent.class));
    }
}
