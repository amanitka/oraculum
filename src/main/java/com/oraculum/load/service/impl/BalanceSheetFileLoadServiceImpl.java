package com.oraculum.load.service.impl;

import com.oraculum.load.dto.LoadParquetDto;
import com.oraculum.load.service.ParquetFileLoadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("balance_sheet")
@RequiredArgsConstructor
public class BalanceSheetFileLoadServiceImpl implements ParquetFileLoadService {

    private static final String TARGET_TABLE_NAME = "t_balance_sheet";
    private static final String BULK_UPSERT_SQL = """
            INSERT INTO t_balance_sheet AS dest
              (id,
               company_id,
               market,
               ticker,
               template,
               variant,
               currency,
               fiscal_year,
               fiscal_period,
               report_date,
               publish_date,
               restated_date,
               extracted_at,
               statement_data,
               created_at,
               updated_at)
            SELECT
               src.id,
               CAST(src.company_id AS INTEGER),
               src.market,
               src.ticker,
               UPPER(src.template),
               UPPER(src.variant),
               src.currency,
               CAST(src.fiscal_year AS INTEGER),
               src.fiscal_period,
               CAST(src.report_date AS DATE),
               CAST(src.publish_date AS DATE),
               CAST(src.restated_date AS DATE),
               CAST(src.extracted_at AS TIMESTAMP),
               CAST(src.statement_data AS JSONB),
               CAST(src.created_at AS TIMESTAMPTZ),
               CAST(src.updated_at AS TIMESTAMPTZ)
            FROM %s AS src
            ON CONFLICT (id)
            DO UPDATE SET
               company_id = EXCLUDED.company_id,
               market = EXCLUDED.market,
               ticker = EXCLUDED.ticker,
               currency = EXCLUDED.currency,
               report_date = EXCLUDED.report_date,
               publish_date = EXCLUDED.publish_date,
               restated_date = EXCLUDED.restated_date,
               extracted_at = EXCLUDED.extracted_at,
               statement_data = EXCLUDED.statement_data,
               updated_at = CURRENT_TIMESTAMP;
            """;

    private final PostgresParquetFileLoader postgresParquetFileLoader;

    @Override
    public void merge(String parquetFilePath) {
        var stagingTableName = PostgresParquetFileLoader.getStagingTableName(TARGET_TABLE_NAME);
        var loadParquetDto = LoadParquetDto.builder()
                .targetTableName(TARGET_TABLE_NAME)
                .stagingTableName(stagingTableName)
                .parquetFilePath(PostgresParquetFileLoader.normalizeAndValidate(parquetFilePath))
                .loadSql(BULK_UPSERT_SQL.formatted(stagingTableName))
                .hasStatementData(true)
                .build();
        postgresParquetFileLoader.loadParquetIntoTargetTable(loadParquetDto);
    }
}