package com.oraculum.loader.service.impl;

import com.oraculum.loader.service.ParquetFileLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("share_price")
@RequiredArgsConstructor
public class SharePriceFileLoaderImpl implements ParquetFileLoader {

    private static final String BULK_UPSERT_SQL = """
            INSERT INTO pg.t_share_price
              (ticker,
               sim_fin_id,
               currency,
               market,
               trade_date,
               open,
               high,
               low,
               close,
               adj_close,
               volume,
               shares_outstanding,
               dividend,
               extracted_at,
               created_at,
               updated_at)
            SELECT
               ticker,
               sim_fin_id,
               currency,
               market,
               trade_date,
               open,
               high,
               low,
               close,
               adj_close,
               volume,
               shares_outstanding,
               dividend,
               extracted_at,
               CURRENT_TIMESTAMP,
               CURRENT_TIMESTAMP
            FROM read_parquet('%s')
            ON CONFLICT (ticker, market, trade_date)
            DO UPDATE SET
               sim_fin_id = EXCLUDED.sim_fin_id,
               currency = EXCLUDED.currency,
               open = EXCLUDED.open,
               high = EXCLUDED.high,
               low = EXCLUDED.low,
               close = EXCLUDED.close,
               adj_close = EXCLUDED.adj_close,
               volume = EXCLUDED.volume,
               shares_outstanding = EXCLUDED.shares_outstanding,
               dividend = EXCLUDED.dividend,
               extracted_at = EXCLUDED.extracted_at,
               updated_at = CURRENT_TIMESTAMP;
            """;
    private final PostgresParquetFileLoader postgresParquetFileLoader;

    @Override
    public void merge(String parquetFilePath) {
        String safeParquetPath = PostgresParquetFileLoader.normalizeAndValidate(parquetFilePath);
        postgresParquetFileLoader.executeBulkSql("Share Price Parquet Ingestion",
                BULK_UPSERT_SQL.formatted(safeParquetPath));
    }
}
