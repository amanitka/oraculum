package com.oraculum.harvester.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Jacksonized
public class FetchSecDocumentsRequest extends HarvesterRequest {

    @JsonProperty("items")
    private final List<TickerDocumentItem> items;

    @Override
    @JsonProperty("request_type")
    public String getRequestType() {
        return "fetch_sec_documents";
    }

    @Getter
    @Builder
    @Jacksonized
    public static class TickerDocumentItem {
        @JsonProperty("ticker")
        private final String ticker;

        @Builder.Default
        @JsonProperty("market")
        private final String market = "US";

        @JsonProperty("document_types")
        private final List<DocumentTypeRequest> documentTypes;
    }

    @Getter
    @Builder
    @Jacksonized
    public static class DocumentTypeRequest {
        @JsonProperty("document_type")
        private final String documentType;

        @JsonProperty("last_processed_file_date")
        private final LocalDate lastProcessedFileDate;
    }
}
