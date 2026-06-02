package com.oraculum.analyst.listener;

import com.oraculum.analyst.domain.AnalysisStatus;
import com.oraculum.analyst.domain.CompanyAnalysisEntity;
import com.oraculum.analyst.listener.message.AnalyzeCompanyRequest;
import com.oraculum.analyst.repository.CompanyAnalysisRepository;
import com.oraculum.analyst.service.CompanyAnalysisWorkflow;
import com.oraculum.analyst.service.dto.AnalysisResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyAnalysisRequestListener {

    private final CompanyAnalysisWorkflow workflow;
    private final CompanyAnalysisRepository repository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${oraculum.kafka.topics.analyst-request}", groupId = "${oraculum.kafka.consumer-group}")
    @Transactional
    public void handleAnalyzeCompanyRequest(AnalyzeCompanyRequest request) {
        log.info("Handling company analysis request for {}", request.ticker());

        try {
            CompanyAnalysisEntity entity = new CompanyAnalysisEntity();
            entity.setId(request.correlationId());
            entity.setTicker(request.ticker());
            entity.setMarket(request.market());
            entity.setAnalysisDate(request.asOf() != null ? request.asOf() : LocalDate.now());
            entity.setStatus(AnalysisStatus.PENDING.name());
            repository.save(entity);

            entity.setStatus(AnalysisStatus.RUNNING.name());
            repository.save(entity);

            AnalysisResultDto result =
                    workflow.run(new com.oraculum.analyst.service.dto.AnalyzeCompanyRequest(request.ticker(),
                    request.market(),
                    request.asOf(),
                    request.defaultVariant()), request.correlationId());

            entity.setStatus(result.status().name());
            entity.setReport(result.reportMd());
            entity.setOutlook(result.outlook());
            entity.setRecommendation(result.recommendation());
            entity.setConviction(result.conviction());
            entity.setAnalysisData(objectMapper.writeValueAsString(result.agentTrace()));
            entity.setError(result.error());
            repository.save(entity);

            log.info("Successfully completed analysis for {}", request.ticker());
        } catch (Exception e) {
            log.error("Analysis failed for {}: {}", request.ticker(), e.getMessage(), e);
            repository.findById(request.correlationId()).ifPresent(entity -> {
                entity.setStatus(AnalysisStatus.FAILED.name());
                entity.setError(e.getMessage());
                repository.save(entity);
            });
        }
    }
}