package com.oraculum.load.service.impl;

import com.oraculum.load.dto.DataFileReadyEvent;
import com.oraculum.load.dto.LoadParquetDto;
import com.oraculum.load.service.ParquetFileLoadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("company")
@RequiredArgsConstructor
public class CompanyFileLoadServiceImpl implements ParquetFileLoadService {

    private static final String TARGET_TABLE_NAME = "t_company";
    private static final String BULK_UPSERT_SQL = """
            INSERT INTO t_company AS dest
              (id,
               ticker,
               market,
               company_name,
               industry_id,
               industry_name,
               sector_name,
               isin,
               description,
               employee_count,
               currency,
               cik,
               extracted_at,
               created_at,
               updated_at)
            SELECT
               CAST(src.id AS INTEGER),
               src.ticker,
               src.market,
               src.company_name,
               CAST(src.industry_id AS INTEGER),
               src.industry_name,
               src.sector_name,
               src.isin,
               src.description,
               CAST(src.employee_count AS BIGINT),
               src.currency,
               src.cik,
               CAST(src.extracted_at AS TIMESTAMP),
               CAST(src.created_at AS TIMESTAMPTZ),
               CAST(src.updated_at AS TIMESTAMPTZ)
            FROM %s AS src
            ON CONFLICT (id)
            DO UPDATE SET
               ticker = EXCLUDED.ticker,
               market = EXCLUDED.market,
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

    @Override
    public void merge(DataFileReadyEvent event) {
        var stagingTableName = PostgresParquetFileLoader.getStagingTableName(TARGET_TABLE_NAME);
        var loadParquetDto = LoadParquetDto.builder()
                .targetTableName(TARGET_TABLE_NAME)
                .stagingTableName(stagingTableName)
                .parquetFilePath(postgresParquetFileLoader.resolveAndValidatePath(event))
                .loadSql(BULK_UPSERT_SQL.formatted(stagingTableName))
                .hasStatementData(false)
                .build();
        postgresParquetFileLoader.loadParquetIntoTargetTable(loadParquetDto);
    }
}
