package com.oraculum.loader.service.impl;

import com.oraculum.loader.service.ParquetFileLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Slf4j
@Component("share_price")
@RequiredArgsConstructor
public class SharePriceFileLoaderImpl implements ParquetFileLoader {

    private static final String BULK_UPSERT_SQL = """
            INSERT INTO t_share_price AS dest
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
               src.ticker,
               CAST(src.sim_fin_id AS INTEGER),
               src.currency,
               src.market,
               CAST(src.trade_date AS DATE),
               CAST(src.open AS DOUBLE PRECISION),
               CAST(src.high AS DOUBLE PRECISION),
               CAST(src.low AS DOUBLE PRECISION),
               CAST(src.close AS DOUBLE PRECISION),
               CAST(src.adj_close AS DOUBLE PRECISION),
               CAST(src.volume AS BIGINT),
               CAST(src.shares_outstanding AS BIGINT),
               CAST(src.dividend AS DOUBLE PRECISION),
               CAST(src.extracted_at AS TIMESTAMP),
               src.created_at,
               src.updated_at
            FROM %s AS src
            ON CONFLICT (ticker, market, trade_date)
            DO UPDATE SET
               sim_fin_id = EXCLUDED.sim_fin_id,
               currency = EXCLUDED.currency,
               "open" = EXCLUDED.open,
               high = EXCLUDED.high,
               low = EXCLUDED.low,
               "close" = EXCLUDED.close,
               adj_close = EXCLUDED.adj_close,
               volume = EXCLUDED.volume,
               shares_outstanding = EXCLUDED.shares_outstanding,
               dividend = EXCLUDED.dividend,
               extracted_at = EXCLUDED.extracted_at,
               updated_at = CURRENT_TIMESTAMP;
            """;

    private final PostgresParquetFileLoader postgresParquetFileLoader;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void merge(String parquetFilePath) {
        String safeParquetPath = PostgresParquetFileLoader.normalizeAndValidate(parquetFilePath);
        String stagingTableName = null;

        try {
            // 1. DuckDB Phase: High-speed load into a temporary Postgres staging table
            stagingTableName = postgresParquetFileLoader.loadParquetIntoStaging(safeParquetPath, "t_share_price");

            // 2. Native Postgres Phase: Execute the UPSERT from the staging table using JdbcTemplate
            log.info("Executing native UPSERT from staging table '{}' to t_share_price", stagingTableName);
            int rowsAffected = jdbcTemplate.update(BULK_UPSERT_SQL.formatted(stagingTableName));
            log.info("Native UPSERT completed successfully. {} rows affected or updated.", rowsAffected);
        } catch (SQLException e) {
            log.error("Failed during DuckDB staging process for file: {}", safeParquetPath, e);
            throw new RuntimeException("Merge process failed during staging", e);
        } catch (Exception e) {
            log.error("Failed during native Postgres UPSERT process for file: {}", safeParquetPath, e);
            throw new RuntimeException("Merge process failed during upsert", e);
        } finally {
            // 3. ALWAYS clean up the staging table using JdbcTemplate
            if (stagingTableName != null) {
                try {
                    log.info("Dropping staging table '{}'", stagingTableName);
                    jdbcTemplate.execute("DROP TABLE IF EXISTS " + stagingTableName + ";");
                } catch (Exception e) {
                    log.error("CRITICAL: Failed to drop staging table '{}'. Manual cleanup required.",
                            stagingTableName, e);
                }
            }
        }
    }
}
