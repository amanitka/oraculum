package com.oraculum.analyst.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.oraculum.company.api.dto.TickerDocumentDto;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.oraculum.company.api.domain.TickerDocumentType;

import java.time.LocalDate;

public record TickerDocumentSlim(
        @JsonProperty("citation_id") String citationId,
        @JsonProperty("document_type") TickerDocumentType documentType,
        @JsonProperty("report_period") LocalDate reportPeriod,
        @JsonProperty("filing_date") LocalDate filingDate,
        @JsonProperty("sentiment_score") Float sentimentScore,
        @JsonProperty("summary") JsonNode summary
) {
    public static TickerDocumentSlim from(TickerDocumentDto dto, String citationId, JsonMapper jsonMapper) {
        if (dto == null) return null;
        JsonNode summaryNode = null;
        if (dto.getSummary() != null) {
            try {
                summaryNode = jsonMapper.readTree(dto.getSummary());
            } catch (Exception e) {
                summaryNode = jsonMapper.getNodeFactory().stringNode(dto.getSummary());
            }
        }
        return new TickerDocumentSlim(
                citationId,
                dto.getDocumentType(),
                dto.getReportPeriod(),
                dto.getFilingDate(),
                dto.getSentimentScore(),
                summaryNode
        );
    }
}
