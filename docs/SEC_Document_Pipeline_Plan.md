# Advanced SEC Document Pipeline — Final Implementation Plan

This architecture cleanly separates **source-specific fetching** (SEC/EDGAR) from **source-agnostic storage** (Oraculum database). It incorporates advanced pipeline features like globally unique accession tracking, URL lineage, and structured warning codes.

## Naming & Architecture Decisions

| Element | Name | Rationale |
|---------|------|-----------|
| **Kafka Request Type** | `fetch_sec_documents` | Source-specific. Triggers the SEC Python Harvester. |
| **Python Service** | `SecDocumentService` | Source-specific logic (edgartools). |
| **Java Enums** | `SecDocumentType`, `SecDocumentSubtype` | Source-specific constants (e.g., `8K`, `10K`, `ITEM_1A`). |
| **Raw Table** | `t_ticker_document_raw` | Generic. Can hold SEC filings, IR transcripts, or Bloomberg data. |
| **Sync Table** | `t_ticker_document_sync_status` | Generic. Tracks sync state per ticker, source, and document type. |
| **Spring `@Service`** | `"ticker_document"` | Generic. Matches the `dataset` field in the Kafka payload. |

---

## 1. Java Request (Kafka → Python)
Java sends a batch of tickers, requesting specific document types and providing the High-Water Mark.

```json
{
  "request_type": "fetch_sec_documents",
  "items": [
    {
      "ticker": "AAPL",
      "market": "US",
      "document_types": [
        { "document_type": "8K", "last_processed_file_date": "2026-02-01" },
        { "document_type": "10K", "last_processed_file_date": "2025-12-31" }
      ]
    }
  ]
}
```

## 2. Python Processing (Single-Pass)
1. Python receives the batch.
2. For **10K**, it downloads the filing *once* and extracts both `ITEM_1A` and `ITEM_7`.
3. It computes a **deterministic ID** for each extracted section using the globally unique SEC accession number:
   `id = SHA256(source + accession_number + document_subtype)`
4. It writes the data to a Parquet file.

## 3. Kafka Response (Python → Java)
Python sends ONE status per ticker/document_type back to Java.
> **Note on Partial Extractions:** If a 10-K is parsed but ITEM_7 fails, the status is `COMPLETED` but with an `extraction_status` of `PARTIAL`. We do not fail the sync run, as retrying the exact same broken file tomorrow won't fix a parser bug.

```json
"refresh_status": [
  {"ticker": "AAPL", "market": "US", "source": "SEC_EDGAR", "document_type": "8K",  "latest_processed_filing_date": "2026-02-15", "status": "COMPLETED", "extraction_status": "FULL",  "message": null},
  {"ticker": "AAPL", "market": "US", "source": "SEC_EDGAR", "document_type": "10K", "latest_processed_filing_date": "2026-01-15", "status": "COMPLETED", "extraction_status": "PARTIAL", "message": "ITEM_1A not present (SRC Exemption)"},
  {"ticker": "MSFT", "market": "US", "source": "SEC_EDGAR", "document_type": "8K",  "latest_processed_filing_date": null,         "status": "COMPLETED", "extraction_status": "EMPTY",   "message": "No new filing found"}
]
```

## 4. Database: `t_ticker_document_raw` (V17)
The schema now includes `accession_number` and `source_url`. The deterministic `id` ensures perfectly idempotent UPSERTs. Partitioning by `report_period` ensures financial periods are logically grouped together, making financial queries highly efficient. Amendments (e.g. `10-K/A`) naturally get their own rows because the SEC assigns them new accession numbers.

```sql
CREATE TABLE t_ticker_document_raw (
    id               VARCHAR(64)  NOT NULL, -- SHA256 Hash
    ticker           VARCHAR(10)  NOT NULL,
    market           VARCHAR(10)  NOT NULL DEFAULT 'US',
    source           VARCHAR(50)  NOT NULL, -- 'SEC_EDGAR'
    document_type    VARCHAR(20)  NOT NULL, -- '8K', '10K'
    document_subtype VARCHAR(50)  NOT NULL, -- 'EX99_1', 'ITEM_1A', 'ITEM_7'
    accession_number VARCHAR(50),           -- SEC filing unique ID
    source_url       VARCHAR(2048),         -- Link to original document
    report_period    DATE         NOT NULL, -- Used for Partitioning (e.g., Fiscal Quarter End)
    filing_date      DATE         NOT NULL,
    content          TEXT         NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    extracted_at     TIMESTAMPTZ  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, report_period)
) PARTITION BY RANGE (report_period);

-- Fast lookup for the LLM processing queue
CREATE INDEX ix_ticker_document_raw_queue
    ON t_ticker_document_raw(ticker, market, source, document_type, document_subtype, report_period)
    WHERE status = 'PENDING';
```

## 5. Database: `t_ticker_document_sync_status` (V18)
Tracks the exact sync state, now including structured warning codes.

```sql
CREATE TABLE t_ticker_document_sync_status (
    ticker                   VARCHAR(10)  NOT NULL,
    market                   VARCHAR(10)  NOT NULL DEFAULT 'US',
    source                   VARCHAR(50)  NOT NULL,         -- 'SEC_EDGAR'
    document_type            VARCHAR(20)  NOT NULL,         -- '8K', '10K'
    status                   VARCHAR(20)  NOT NULL,         -- 'COMPLETED', 'FAILED'
    extraction_status        VARCHAR(20),                   -- 'FULL', 'PARTIAL', 'EMPTY'
    message                  VARCHAR(255),                  -- Optional human-readable note
    last_processed_file_date DATE,                          -- High Water Mark sent to Python
    last_refresh_date        TIMESTAMPTZ  NOT NULL,         -- When we last asked Python
    last_file_refresh_date   TIMESTAMPTZ,                   -- When Python last found something new
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ticker, market, source, document_type)
);
```

---

## Execution Checklist

### 🐍 Python Harvester (`oraculum-harvestor`) [COMPLETED]
- [x] Rename `common/requests/earnings_transcript.py` → `sec_documents.py` (Create `FetchSecDocumentsRequest`).
- [x] Rename `harvester/services/earnings_transcript.py` → `sec_document.py` (Create `SecDocumentService`).
- [x] Implement deterministic ID generation using `accession_number`.
- [x] Collect `accession_number`, `source_url`, and `report_period` from `edgartools`.
- [x] Implement `extraction_status` reporting logic (FULL, PARTIAL, EMPTY).
- [x] Update `DataFileReadyEvent` to accept the `refresh_status` array.
- [x] Wire the new service in `harvester/subscribers/request.py`.

### ☕ Java Backend (`oraculum`) [COMPLETED]
- [x] Rename `V17__t_sec_document_raw.sql` to `V17__t_ticker_document_raw.sql` and update schema (`accession_number`, `source_url`, `report_period`).
- [x] Create `V18__t_ticker_document_sync_status.sql` (`extraction_status`).
- [x] Create `TickerDocumentType` and `TickerDocumentSubtype` Enums.
- [x] Update `TickerDocumentFileLoadServiceImpl.java` to `@Service("ticker_document")` and adjust the DuckDB bulk upsert query.
- [x] Create `TickerDocumentSyncStatusService.java` to read `refresh_status` from the Kafka event and update V18.
- [x] Update `DataFileReadyEvent.java` to map the new `refresh_status` array.
