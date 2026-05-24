package com.oraculum.loader.service.impl;

import com.oraculum.loader.dto.LoadParquetDto;
import com.oraculum.loader.service.ParquetFileLoader;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("income_statement")
@RequiredArgsConstructor
public class IncomeStatementFileLoaderImpl implements ParquetFileLoader {

    private static final String TARGET_TABLE_NAME = "t_income_statement";
    private static final String BULK_UPSERT_SQL = """
            INSERT INTO t_income_statement AS dest
              (composite_key,
               template,
               variant,
               ticker,
               simfin_id,
               currency,
               fiscal_year,
               fiscal_period,
               report_date,
               publish_date,
               restated_date,
               extracted_at,
               payload,
               created_at,
               updated_at)
            SELECT
               src.composite_key,
               src.template,
               src.variant,
               src.ticker,
               CAST(src.simfin_id AS INTEGER),
               src.currency,
               CAST(src.fiscal_year AS INTEGER),
               src.fiscal_period,
               CAST(src.report_date AS DATE),
               CAST(src.publish_date AS DATE),
               CAST(src.restated_date AS DATE),
               CAST(src.extracted_at AS TIMESTAMP),
               CAST(src.payload AS JSONB),
               src.created_at,
               src.updated_at
            FROM %s AS src
            ON CONFLICT (composite_key)
            DO UPDATE SET
               simfin_id = EXCLUDED.simfin_id,
               currency = EXCLUDED.currency,
               report_date = EXCLUDED.report_date,
               publish_date = EXCLUDED.publish_date,
               restated_date = EXCLUDED.restated_date,
               extracted_at = EXCLUDED.extracted_at,
               payload = EXCLUDED.payload,
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
