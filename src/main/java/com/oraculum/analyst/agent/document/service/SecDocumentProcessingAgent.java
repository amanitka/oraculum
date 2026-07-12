package com.oraculum.analyst.agent.document.service;

import com.oraculum.analyst.agent.document.dto.SecEx991Response;
import com.oraculum.analyst.agent.document.dto.SecMdResponse;
import com.oraculum.analyst.agent.document.dto.SecRfResponse;
import com.oraculum.analyst.api.SecDocumentProcessingApi;
import com.oraculum.analyst.config.PromptRegistry;
import com.oraculum.analyst.domain.PromptType;
import com.oraculum.company.api.CompanyTickerDocumentApi;
import com.oraculum.company.api.dto.TickerDocumentDto;
import com.oraculum.company.api.dto.TickerDocumentRawDto;
import com.oraculum.llm.api.LlmCallRequest;
import com.oraculum.llm.api.LlmRouterApi;
import com.oraculum.llm.api.dto.CorrelationType;
import com.oraculum.llm.api.dto.LlmProviderType;
import com.oraculum.llm.api.dto.LlmTierType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecDocumentProcessingAgent implements SecDocumentProcessingApi {

    private final CompanyTickerDocumentApi companyTickerDocumentApi;
    private final LlmRouterApi llmRouterApi;
    private final PromptRegistry promptRegistry;
    private final JsonMapper jsonMapper;

    @Override
    public int processPendingDocuments(int limit, List<LlmProviderType> providerFallbackOrder) {
        log.info("Starting processing of up to {} pending SEC documents.", limit);
        List<TickerDocumentRawDto> pendingDocs = companyTickerDocumentApi.getPendingRawDocuments(limit);
        int successCount = 0;

        for (TickerDocumentRawDto doc : pendingDocs) {
            try {
                processDocument(doc, providerFallbackOrder);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to process SEC document ID: {}, ticker: {}, period: {}. Error: {}",
                        doc.getId(), doc.getTicker(), doc.getReportPeriod(), e.getMessage(), e);
                try {
                    companyTickerDocumentApi.updateRawDocumentStatus(doc.getId(), doc.getReportPeriod(), "FAILED");
                } catch (Exception ex) {
                    log.error("Failed to mark SEC document {} status as FAILED. Error: {}", doc.getId(), ex.getMessage(), ex);
                }
            }
        }

        log.info("Finished processing batch. Successfully processed {}/{} documents.", successCount, pendingDocs.size());
        return successCount;
    }

    private void processDocument(TickerDocumentRawDto doc, List<LlmProviderType> providerFallbackOrder) throws Exception {
        String prompt = renderPrompt(doc);
        UUID correlationId = UUID.randomUUID();
        LlmResult result = executeLlmCall(doc, prompt, correlationId, providerFallbackOrder);

        TickerDocumentDto summaryDto = TickerDocumentDto.builder()
                .id(doc.getId())
                .ticker(doc.getTicker())
                .market(doc.getMarket())
                .documentType(doc.getDocumentType())
                .documentSubtype(doc.getDocumentSubtype())
                .reportPeriod(doc.getReportPeriod())
                .summary(result.json())
                .sentimentScore(result.sentimentScore())
                .build();

        companyTickerDocumentApi.createDocumentSummary(summaryDto);
        log.info("Successfully processed SEC document ID: {}, ticker: {}, subtype: {}, sentiment: {}",
                doc.getId(), doc.getTicker(), doc.getDocumentSubtype(), result.sentimentScore());
    }

    private String renderPrompt(TickerDocumentRawDto doc) {
        PromptType promptType = PromptType.valueOf(doc.getDocumentSubtype().name());
        return promptRegistry.getPrompt(promptType)
                .replace("{{ content }}", doc.getContent() != null ? doc.getContent() : "")
                .replace("{{ ticker }}", doc.getTicker());
    }

    private LlmResult executeLlmCall(TickerDocumentRawDto doc, String prompt, UUID correlationId,
                                     List<LlmProviderType> providerFallbackOrder) {
        return switch (doc.getDocumentSubtype()) {
            case SEC_MD -> executeMdCall(prompt, correlationId, providerFallbackOrder);
            case SEC_RF -> executeRfCall(prompt, correlationId, providerFallbackOrder);
            case SEC_EX99_1 -> executeEx991Call(prompt, correlationId, providerFallbackOrder);
            default -> throw new IllegalArgumentException("Unsupported document subtype: " + doc.getDocumentSubtype());
        };
    }

    private LlmResult executeMdCall(String prompt, UUID correlationId,
                                    List<LlmProviderType> providerFallbackOrder) {
        LlmCallRequest<SecMdResponse> request = LlmCallRequest.withFallbackOverride(
                LlmTierType.MINI,
                prompt,
                SecMdResponse.class,
                correlationId,
                CorrelationType.SEC_DOCUMENT_SUMMARY,
                "SecDocumentProcessingService",
                providerFallbackOrder
        );
        var response = llmRouterApi.executeCall(request);
        SecMdResponse mdResponse = response.result();
        float score = mdResponse.sentimentScore() != null ? mdResponse.sentimentScore().floatValue() : 0.0f;
        return new LlmResult(jsonMapper.writeValueAsString(mdResponse), score);
    }

    private LlmResult executeRfCall(String prompt, UUID correlationId,
                                    List<LlmProviderType> providerFallbackOrder) {
        LlmCallRequest<SecRfResponse> request = LlmCallRequest.withFallbackOverride(
                LlmTierType.MINI,
                prompt,
                SecRfResponse.class,
                correlationId,
                CorrelationType.SEC_DOCUMENT_SUMMARY,
                "SecDocumentProcessingService",
                providerFallbackOrder
        );
        var response = llmRouterApi.executeCall(request);
        SecRfResponse rfResponse = response.result();
        float score = rfResponse.sentimentScore() != null ? rfResponse.sentimentScore().floatValue() : 0.0f;
        return new LlmResult(jsonMapper.writeValueAsString(rfResponse), score);
    }

    private LlmResult executeEx991Call(String prompt, UUID correlationId,
                                       List<LlmProviderType> providerFallbackOrder) {
        LlmCallRequest<SecEx991Response> request = LlmCallRequest.withFallbackOverride(
                LlmTierType.MINI,
                prompt,
                SecEx991Response.class,
                correlationId,
                CorrelationType.SEC_DOCUMENT_SUMMARY,
                "SecDocumentProcessingService",
                providerFallbackOrder
        );
        var response = llmRouterApi.executeCall(request);
        SecEx991Response exResponse = response.result();
        float score = exResponse.sentimentScore() != null ? exResponse.sentimentScore().floatValue() : 0.0f;
        return new LlmResult(jsonMapper.writeValueAsString(exResponse), score);
    }

    private record LlmResult(String json, float sentimentScore) {
    }
}
