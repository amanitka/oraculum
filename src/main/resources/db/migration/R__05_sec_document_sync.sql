-- Flyway repeatable migration script for SEC document sync views

DROP VIEW IF EXISTS v_ticker_sec_document_stale_sync CASCADE;

-- =================================================================
-- VIEW: v_ticker_sec_document_stale_sync
-- Description: Identifies US companies and specific SEC document types
--              (8K, 10K) that are stale and need to be refreshed.
-- =================================================================
DO $$ BEGIN RAISE NOTICE 'Creating view: v_ticker_sec_document_stale_sync'; END $$;

CREATE VIEW v_ticker_sec_document_stale_sync AS
SELECT
    c.ticker,
    c.market,
    c.cik,
    dt.document_type,
    tds.last_processed_file_date,
    tds.last_file_refresh_at,
    tds.last_refresh_at
FROM t_company c
CROSS JOIN (SELECT '8K' AS document_type
            UNION ALL
            SELECT '10K' AS document_type
            ) dt
LEFT JOIN t_ticker_document_sync_status tds ON tds.ticker = c.ticker
                                           AND tds.market = UPPER(c.market)
                                           AND tds.document_type = dt.document_type
                                           AND tds.source = 'SEC'
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
