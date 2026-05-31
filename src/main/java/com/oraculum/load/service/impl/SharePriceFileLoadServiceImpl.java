package com.oraculum.load.service.impl;

import com.oraculum.load.dto.LoadParquetDto;
import com.oraculum.load.service.ParquetFileLoadService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("share_price")
@RequiredArgsConstructor
public class SharePriceFileLoadServiceImpl implements ParquetFileLoadService {

    private static final String TARGET_TABLE_NAME = "t_share_price";
    private static final String BULK_UPSERT_SQL = """
            INSERT INTO t_share_price AS dest
              (company_id,
               trade_date,
               market,
               ticker,
               currency,
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
               CAST(src.company_id AS INTEGER),
               CAST(src.trade_date AS DATE),
               src.market,
               src.ticker,
               src.currency,
               CAST(src.open AS DOUBLE PRECISION),
               CAST(src.high AS DOUBLE PRECISION),
               CAST(src.low AS DOUBLE PRECISION),
               CAST(src.close AS DOUBLE PRECISION),
               CAST(src.adj_close AS DOUBLE PRECISION),
               CAST(src.volume AS BIGINT),
               CAST(src.shares_outstanding AS BIGINT),
               CAST(src.dividend AS DOUBLE PRECISION),
               CAST(src.extracted_at AS TIMESTAMP),
               CAST(src.created_at AS TIMESTAMPTZ),
               CAST(src.updated_at AS TIMESTAMPTZ)
            FROM %s AS src
            ON CONFLICT (company_id, trade_date)
            DO UPDATE SET
               market = EXCLUDED.market,
               ticker = EXCLUDED.ticker,
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
                LoadParquetDto.builder().targetTableName(TARGET_TABLE_NAME).stagingTableName(stagingTableName).parquetFilePath(PostgresParquetFileLoader.normalizeAndValidate(parquetFilePath)).loadSql(BULK_UPSERT_SQL.formatted(stagingTableName)).hasPayload(false).build();
        postgresParquetFileLoader.loadParquetIntoTargetTable(loadParquetDto);
    }
}