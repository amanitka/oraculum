# Harvester Update: Remove `market` from News Data

The data model for news has been simplified. The `market` field is no longer needed for news-related tickers.

## Action Required

The harvester's transformation logic for the `news` dataset should be updated to:

1.  **Remove** the `market` column from the Parquet files generated for the `news_ticker` dataset.
2.  The Kafka message for news data will no longer contain the `market` field.

This change simplifies the news data pipeline and removes a redundant field.
