-- Flyway repeatable migration script for SEC ticker document views

-- =================================================================
-- VIEW: v_ticker_sec_document_stale_sync
-- Description: Identifies US companies and specific SEC document types
--              (8K, 10K) that are stale and need to be refreshed.
-- =================================================================
DROP VIEW IF EXISTS public.v_ticker_sec_document_stale_sync CASCADE;

DO $$ BEGIN RAISE NOTICE 'Creating view: v_ticker_sec_document_stale_sync'; END $$;

CREATE VIEW public.v_ticker_sec_document_stale_sync AS
SELECT
    c.ticker,
    c.market,
    c.cik,
    dt.document_type,
    tds.last_processed_file_date,
    tds.last_file_refresh_at,
    tds.last_refresh_at
FROM public.t_company c
CROSS JOIN (SELECT 'SEC_8K' AS document_type
            UNION ALL
            SELECT 'SEC_10K' AS document_type
            ) dt
LEFT JOIN public.t_ticker_document_sync_status tds ON tds.ticker = c.ticker
                                                 AND tds.market = c.market
                                                 AND tds.document_type = dt.document_type
                                                 AND tds.source = 'SEC_EDGAR'
WHERE c.market = 'us'
  AND (tds.last_refresh_at IS NULL
       OR (tds.last_refresh_at IS NOT NULL
           AND tds.last_processed_file_date IS NOT NULL
           AND tds.last_refresh_at < NOW() - INTERVAL '30 DAYS'
           AND COALESCE(tds.last_file_refresh_at, '1970-01-01'::timestamptz) < NOW() - INTERVAL '30 DAYS')
       OR (tds.last_refresh_at IS NOT NULL
           AND tds.last_processed_file_date IS NULL
           AND tds.last_refresh_at < NOW() - INTERVAL '90 DAYS')
       );

-- =================================================================
-- VIEW: v_ticker_document
-- Description: Clean, deduplicated, and fresh view of processed SEC document summaries.
--              Groups duplicates/amendments, keeping the latest one per period,
--              and excludes stale documents (8-K > 1 year, others > 2 years).
-- =================================================================
DROP VIEW IF EXISTS public.v_ticker_document CASCADE;

DO $$ BEGIN RAISE NOTICE 'Creating view: v_ticker_document'; END $$;

CREATE VIEW public.v_ticker_document AS
WITH DeduplicatedDocs AS (
    SELECT 
        d.id,
        d.ticker,
        d.market,
        d.document_type,
        d.document_subtype,
        d.report_period,
        d.filing_date,
        d.source_url,
        d.accession_number,
        d.summary,
        d.sentiment_score,
        d.created_at,
        d.updated_at,
        ROW_NUMBER() OVER (
            PARTITION BY d.ticker, d.market, d.document_type, d.document_subtype, d.report_period
            ORDER BY d.filing_date DESC, d.created_at DESC
        ) as period_rn
    FROM public.t_ticker_document d
    WHERE d.report_period >= CURRENT_DATE - CASE 
        WHEN d.document_type = 'SEC_8K' THEN INTERVAL '1 year'
        ELSE INTERVAL '2 years'
    END
)
SELECT 
    id,
    ticker,
    market,
    document_type,
    document_subtype,
    report_period,
    filing_date,
    source_url,
    accession_number,
    summary,
    sentiment_score,
    created_at,
    updated_at
FROM DeduplicatedDocs
WHERE period_rn = 1;
