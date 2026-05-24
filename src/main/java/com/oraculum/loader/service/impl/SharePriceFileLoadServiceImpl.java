package com.oraculum.loader.service.impl;

import com.oraculum.loader.dto.LoadParquetDto;
import com.oraculum.loader.service.ParquetFileLoadService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("share_price")
@RequiredArgsConstructor
public class SharePriceFileLoadServiceImpl implements ParquetFileLoadService {

    private static final String TARGET_TABLE_NAME = "t_share_price";
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

    @Transactional
    @Override
    public void merge(String parquetFilePath) {
        var stagingTableName = PostgresParquetFileLoader.getStagingTableName(TARGET_TABLE_NAME);
        var loadParquetDto =
                LoadParquetDto.builder().targetTableName(TARGET_TABLE_NAME).stagingTableName(stagingTableName).parquetFilePath(PostgresParquetFileLoader.normalizeAndValidate(parquetFilePath)).loadSql(BULK_UPSERT_SQL.formatted(stagingTableName)).build();
        postgresParquetFileLoader.loadParquetIntoTargetTable(loadParquetDto);
    }
}
