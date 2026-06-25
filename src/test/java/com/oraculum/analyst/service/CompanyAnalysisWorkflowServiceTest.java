package com.oraculum.analyst.service;

import com.oraculum.analyst.agent.dto.AgentOutput;
import com.oraculum.analyst.agent.dto.CriticAgentOutput;
import com.oraculum.analyst.agent.dto.SynthesizerAgentOutput;
import com.oraculum.analyst.agent.service.Agent;
import com.oraculum.analyst.api.domain.AgentType;
import com.oraculum.analyst.api.domain.AnalysisOutlook;
import com.oraculum.analyst.api.domain.AnalysisRecommendation;
import com.oraculum.analyst.api.dto.CompanyAnalysisRequestEvent;
import com.oraculum.analyst.api.event.CompanyAnalysisProgressEvent;
import com.oraculum.analyst.config.AnalystProperties;
import com.oraculum.analyst.dto.CompanyAnalysisResult;
import com.oraculum.analyst.dto.CompanyFactSheetData;
import com.oraculum.company.api.CompanyMetadataApi;
import com.oraculum.company.api.dto.CompanyDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompanyAnalysisWorkflowServiceTest {

    @Mock
    private CompanyMetadataApi companyMetadataApi;

    @Mock
    private AnalystProperties analystProperties;

    @Mock
    private CompanyFactSheetDataService companyFactSheetDataService;

    @Mock
    private Map<AgentType, Agent<?>> agents;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CompanyAnalysisWorkflowService workflowService;

    private CompanyAnalysisRequestEvent request;

    @BeforeEach
    void setUp() {
        request = new CompanyAnalysisRequestEvent(
                UUID.randomUUID(),
                1,
                "AAPL",
                "US",
                LocalDate.now(),
                "focus"
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
        CompanyDto companyDto = mock(CompanyDto.class);
        when(companyMetadataApi.getCompanyById(1)).thenReturn(companyDto);
        when(companyFactSheetDataService.create(companyDto)).thenReturn(mock(CompanyFactSheetData.class));
        when(analystProperties.tokenBudget()).thenReturn(100000);

        AnalystProperties.Critic criticProps = mock(AnalystProperties.Critic.class);
        when(analystProperties.critic()).thenReturn(criticProps);
        when(criticProps.maxReruns()).thenReturn(3);
        when(criticProps.maxSpecialistsPerRerun()).thenReturn(2);

        // mock critic output
        Agent criticAgent = mock(Agent.class);
        when(agents.get(AgentType.CRITIC)).thenReturn(criticAgent);
        AgentOutput criticOutput = new AgentOutput(
                new CriticAgentOutput(List.of("Consolidated feedback"), true, null), 100
        );
        when(criticAgent.run(any())).thenReturn(criticOutput);

        // mock synthesizer output
        Agent synthesizerAgent = mock(Agent.class);
        when(agents.get(AgentType.SYNTHESIZER)).thenReturn(synthesizerAgent);
        AgentOutput synthOutput = new AgentOutput(
                new SynthesizerAgentOutput("Report", AnalysisOutlook.BULLISH, AnalysisRecommendation.BUY, 80, null, null), 200
        );
        when(synthesizerAgent.run(any())).thenReturn(synthOutput);

        CompanyAnalysisResult result = workflowService.run(request);

        assertNotNull(result);
        assertNull(result.error());
        verify(eventPublisher, atLeastOnce()).publishEvent(any(CompanyAnalysisProgressEvent.class));
    }
}
