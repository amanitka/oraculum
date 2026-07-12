package com.oraculum.analyst.api;

import com.oraculum.llm.api.dto.LlmProviderType;

import java.util.List;

public interface SecDocumentProcessingApi {
    /**
     * Process pending SEC raw documents.
     *
     * @param limit maximum number of documents to process in this batch
     * @param providerFallbackOrder LLM provider routing priority to use
     * @return the number of successfully processed documents
     */
    int processPendingDocuments(int limit, List<LlmProviderType> providerFallbackOrder);
}
