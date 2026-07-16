package com.oraculum.analyst.service;

import com.oraculum.analyst.api.domain.AnalysisStatus;
import com.oraculum.analyst.api.dto.CompanyAnalysisRequestEvent;
import com.oraculum.analyst.api.event.CompanyAnalysisProgressEvent;
import com.oraculum.analyst.domain.CompanyAnalysisEntity;
import com.oraculum.analyst.dto.CompanyAnalysisResult;
import com.oraculum.company.api.dto.TickerKeyDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyAnalysisOrchestrationServiceTest {

    @Mock
    private CompanyAnalysisService companyAnalysisService;

    @Mock
    private CompanyAnalysisWorkflowService workflow;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CompanyAnalysisOrchestrationService orchestrationService;

    private CompanyAnalysisRequestEvent request;
    private UUID correlationId;

    @BeforeEach
    void setUp() {
        correlationId = UUID.randomUUID();
        request = new CompanyAnalysisRequestEvent(
                correlationId,
                1,
                new TickerKeyDto("AAPL", "US"),
                LocalDate.now(),
                "focus",
                null
        );
    }

    @Test
    void executeAnalysis_alreadyCompleted_doesNothing() {
        when(companyAnalysisService.isAnalysisCompleted(correlationId)).thenReturn(true);

        orchestrationService.executeAnalysis(request);

        verify(companyAnalysisService, never()).createOrUpdateAnalysis(any());
        verify(workflow, never()).run(any());
    }

    @Test
    void executeAnalysis_success() {
        when(companyAnalysisService.isAnalysisCompleted(correlationId)).thenReturn(false);

        CompanyAnalysisEntity entity = new CompanyAnalysisEntity();
        entity.setId(correlationId);
        when(companyAnalysisService.createOrUpdateAnalysis(any())).thenReturn(entity);

        CompanyAnalysisResult result = CompanyAnalysisResult.builder()
                .status(AnalysisStatus.COMPLETED)
                .build();
        when(workflow.run(request)).thenReturn(result);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        orchestrationService.executeAnalysis(request);

        verify(workflow).run(request);
        verify(companyAnalysisService, times(3)).createOrUpdateAnalysis(any());
        verify(eventPublisher).publishEvent(new CompanyAnalysisProgressEvent(correlationId, null, true));
    }

    @Test
    void executeAnalysis_workflowThrowsException_marksAsFailed() {
        when(companyAnalysisService.isAnalysisCompleted(correlationId)).thenReturn(false);

        CompanyAnalysisEntity entity = new CompanyAnalysisEntity();
        entity.setId(correlationId);
        when(companyAnalysisService.createOrUpdateAnalysis(any())).thenReturn(entity);

        when(workflow.run(request)).thenThrow(new RuntimeException("Workflow error"));

        orchestrationService.executeAnalysis(request);

        verify(workflow).run(request);
        verify(companyAnalysisService, times(3)).createOrUpdateAnalysis(any());
        verify(eventPublisher).publishEvent(new CompanyAnalysisProgressEvent(correlationId, null, true));
    }
}
