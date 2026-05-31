# Harvester, DTOs, and Loader Changes Guide

This guide details the necessary modifications to the data harvester, Data Transfer Objects (DTOs), and loader components to align with the new database schema.

## 1. Harvester Responsibilities and Logic

The harvester's primary role is to transform raw source data (like CSVs) into clean, structured Parquet files that match the target application schema.

### Kafka Request Types

The harvester consumes requests from Kafka. The request type for fetching company data has been changed:
-   **Old Request Type:** `fetch_ticker`
-   **New Request Type:** `fetch_company`

The harvester must be updated to handle the new `fetch_company` request type.

### Field Enrichment and Renaming

-   **Market Column Enrichment:** Some source files (e.g., financial statement CSVs) may not contain a `market` column. The harvester's processing job is often triggered with market context (e.g., as part of the incoming request or job parameters). The harvester **must** use this context to add the correct `market` column to every record in the generated Parquet files.
-   **ID Renaming (`simfin_id` -> `company_id`):** The raw data from SimFin uses `simfin_id`. The harvester **must** rename this field to `company_id` in the output Parquet files for financial statements and share prices. This ensures the Parquet schema directly matches the foreign key names used in the database. For the company data itself, this `simfin_id` should be mapped to the `id` column.

### Key Generation Logic Change

**CRITICAL:** The method for generating the unique ID for financial statement records must be updated. This ID is stored in the `id` column of the financial statement tables (e.g., `t_balance_sheet`).

-   **Old Logic:** The key was likely generated using the `ticker`, e.g., `{ticker}-{fiscal_year}-{fiscal_period}-{variant}`.
-   **New Logic:** The key **must** now be generated using the `sim_fin_id`. For example: `{sim_fin_id}-{fiscal_year}-{fiscal_period}-{variant}`.

This change is essential for ensuring record uniqueness and correctness.

## 2. Parquet File Structure

The final output Parquet files must adhere to the following structure:

-   **Company Data (`t_company`):**
    -   `id` (Integer, from `simfin_id`)
    -   `ticker` (String)
    -   `market` (String, length 10)
    -   `company_name` (String)
    -   `industry_id` (Integer)
    -   `industry_name` (String)
    -   `sector_name` (String)
    -   `isin` (String)
    -   `description` (String)
    -   `employee_count` (Long)
    -   `currency` (String)
    -   `cik` (String)
    -   `extracted_at` (Timestamp)
    -   `created_at` (Timestamp)
    -   `updated_at` (Timestamp)

-   **Financial Statements (`t_balance_sheet`, `t_cash_flow_statement`, `t_income_statement`):**
    -   `id` (String, the new composite key)
    -   `company_id` (Integer, from `simfin_id`)
    -   `market` (String, length 10, enriched by harvester)
    -   `template` (String)
    -   `variant` (String)
    -   `currency` (String)
    -   `fiscal_year` (Integer)
    -   `fiscal_period` (String)
    -   `report_date` (Date)
    -   `publish_date` (Date)
    -   `restated_date` (Date)
    -   `extracted_at` (Timestamp)
    -   `payload` (JSON)
    -   `created_at` (Timestamp)
    -   `updated_at` (Timestamp)

-   **Share Prices (`t_share_price`):**
    -   `company_id` (Integer, from `simfin_id`)
    -   `trade_date` (Date)
    -   `market` (String, length 10, enriched by harvester)
    -   `currency` (String)
    -   `open` (Double)
    -   `high` (Double)
    -   `low` (Double)
    -   `close` (Double)
    -   `adj_close` (Double)
    -   `volume` (Long)
    -   `shares_outstanding` (Long)
    -   `dividend` (Double)
    -   `extracted_at` (Timestamp)
    -   `created_at` (Timestamp)
    -   `updated_at` (Timestamp)

-   **News Data (`t_news_ticker`):**
    -   `news_id` (String)
    -   `ticker` (String)
    -   `market` (String, length 10)
    -   `time_published` (Timestamp)
    -   `relevance_score` (Float)
    -   `ticker_sentiment_score` (Float)
    -   `ticker_sentiment_label` (String)
    -   `created_at` (Timestamp)
    -   `updated_at` (Timestamp)

## 3. Data Transfer Objects (DTOs) and Loader

The DTOs and Loader will consume the clean data from the Parquet files. No changes are needed to the loader logic beyond what was previously described, as it now expects the data to be in the correct format. The DTOs should align with the Parquet file structure.
