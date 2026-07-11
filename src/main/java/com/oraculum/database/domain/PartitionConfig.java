package com.oraculum.database.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PartitionConfig {
    SHARE_PRICE("t_share_price", PartitionType.MONTHLY, 3, 240),
    NEWS("t_news", PartitionType.YEARLY, 24, 60),
    NEWS_TICKER("t_news_ticker", PartitionType.YEARLY, 24, 60),
    INSIDER_TRANSACTION_TICKER("t_insider_transaction_ticker", PartitionType.YEARLY, 24, 120),
    LLM_EXECUTION_LOG("t_llm_execution_log", PartitionType.MONTHLY, 12, 12),
    TickerDocumentFile("t_ticker_document_raw", PartitionType.YEARLY, 24, 120);

    private final String tableName;
    private final PartitionType type;
    private final int monthsAhead;
    private final int monthsToKeep;
}
