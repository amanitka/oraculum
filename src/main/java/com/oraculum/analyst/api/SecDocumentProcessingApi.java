package com.oraculum.analyst.api;

import com.oraculum.company.api.dto.TickerKeyDto;

public interface SecDocumentProcessingApi {
    /**
     * Process pending SEC raw documents.
     *
     * @param limit       maximum number of documents to process in this batch
     * @param maxPriority The maximum priority (e.g. 1 = latest only, 3 = latest 3)
     * @return the number of successfully processed documents
     */
    int processPendingDocuments(int limit, int maxPriority);

    /**
     * Just-In-Time (JIT) processing for a specific ticker before analysis.
     *
     * @param tickerKey   the ticker and market key
     * @param maxPriority The maximum priority (e.g. 1 = latest only)
     * @return the number of successfully processed documents
     */
    int processMissingDocumentsForTicker(TickerKeyDto tickerKey, int maxPriority);
}
