package com.oraculum.loader.service.impl;

import com.oraculum.loader.dto.LoadParquetDto;
import com.oraculum.loader.service.ParquetFileLoadService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("ticker")
@RequiredArgsConstructor
public class TickerFileLoadServiceImpl implements ParquetFileLoadService {

    private static final String TARGET_TABLE_NAME = "t_ticker";
    private static final String BULK_UPSERT_SQL = """
            INSERT INTO t_ticker AS dest
              (ticker,
               provider_id,
               provider_name,
               company_name,
               industry_id,
               industry_name,
               sector_name,
               isin,
               description,
               employee_count,
               market,
               currency,
               cik,
               extracted_at,
               created_at,
               updated_at)
            SELECT
               src.ticker,
               CAST(src.provider_id AS INTEGER),
               src.provider_name,
               src.company_name,
               CAST(src.industry_id AS INTEGER),
               src.industry_name,
               src.sector_name,
               src.isin,
               src.description,
               CAST(src.employee_count AS BIGINT),
               src.market,
               src.currency,
               src.cik,
               CAST(src.extracted_at AS TIMESTAMP),
               src.created_at,
               src.updated_at
            FROM %s AS src
            ON CONFLICT (ticker, market)
            DO UPDATE SET
               provider_id = EXCLUDED.provider_id,
               provider_name = EXCLUDED.provider_name,
               company_name = EXCLUDED.company_name,
               industry_id = EXCLUDED.industry_id,
               industry_name = EXCLUDED.industry_name,
               sector_name = EXCLUDED.sector_name,
               isin = EXCLUDED.isin,
               description = EXCLUDED.description,
               employee_count = EXCLUDED.employee_count,
               currency = EXCLUDED.currency,
               cik = EXCLUDED.cik,
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
