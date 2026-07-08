package com.oraculum.load.service.impl;

import com.oraculum.load.dto.DataFileReadyEvent;
import com.oraculum.load.dto.LoadParquetDto;
import com.oraculum.load.service.ParquetFileLoadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("insider_transaction")
@RequiredArgsConstructor
public class InsiderTransactionTickerFileLoadServiceImpl implements ParquetFileLoadService {

    private static final String TARGET_TABLE_NAME = "t_insider_transaction_ticker";
    private static final String BULK_UPSERT_SQL = """
            INSERT INTO t_insider_transaction_ticker AS dest
              (id,
               ticker,
               insider_name,
               title,
               trade_type,
               currency,
               price,
               qty,
               owned,
               delta_own,
               value,
               filing_date,
               trade_date,
               created_at)
            SELECT
               src.id,
               src.ticker,
               src.insider_name,
               src.title,
               src.trade_type,
               src.currency,
               CAST(src.price AS NUMERIC),
               CAST(src.qty AS NUMERIC),
               CAST(src.owned AS NUMERIC),
               CAST(src.delta_own AS NUMERIC),
               CAST(src.value AS NUMERIC),
               CAST(src.filing_date AS TIMESTAMP),
               CAST(src.trade_date AS DATE),
               CURRENT_TIMESTAMP
            FROM %s AS src
            ON CONFLICT (id, filing_date)
            DO NOTHING;
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
