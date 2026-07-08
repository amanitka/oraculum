package com.oraculum.load.service.impl;

import com.oraculum.company.api.event.TickerDocumentLoadEvent;
import com.oraculum.load.dto.DataFileReadyEvent;
import com.oraculum.load.dto.LoadParquetDto;
import com.oraculum.load.service.ParquetFileLoadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service("ticker_document")
@RequiredArgsConstructor
@Slf4j
public class TickerDocumentFileLoadServiceImpl implements ParquetFileLoadService {

    private static final String TARGET_TABLE_NAME = "t_ticker_document_raw";
    private static final String BULK_UPSERT_SQL = """
            INSERT INTO %s dest
              (id,
               ticker,
               market,
               source,
               document_type,
               document_subtype,
               accession_number,
               source_url,
               report_period,
               filing_date,
               content,
               status,
               extracted_at,
               created_at,
               updated_at)
            SELECT
               id,
               ticker,
               market,
               source,
               document_type,
               document_subtype,
               accession_number,
               source_url,
               report_period,
               filing_date,
               content,
               'PENDING',
               CAST(extracted_at AS TIMESTAMP),
               CURRENT_TIMESTAMP,
               CURRENT_TIMESTAMP
            FROM read_parquet('%s')
            ON CONFLICT (id, report_period)
            DO UPDATE SET
                content = EXCLUDED.content,
                status = EXCLUDED.status,
                updated_at = CURRENT_TIMESTAMP
            """;
    private final PostgresParquetFileLoader postgresParquetFileLoader;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void merge(DataFileReadyEvent event) {
        var stagingTableName = PostgresParquetFileLoader.getStagingTableName(TARGET_TABLE_NAME);
        var loadParquetDto = LoadParquetDto.builder()
                .targetTableName(TARGET_TABLE_NAME)
                .stagingTableName(stagingTableName)
                .parquetFilePath(postgresParquetFileLoader.resolveAndValidatePath(event))
                .loadSql(BULK_UPSERT_SQL.formatted(TARGET_TABLE_NAME, stagingTableName))
                .hasStatementData(false)
                .build();
        postgresParquetFileLoader.loadParquetIntoTargetTable(loadParquetDto);

        if (event.fileStatuses() != null && !event.fileStatuses().isEmpty()) {
            applicationEventPublisher.publishEvent(new TickerDocumentLoadEvent(event.fileStatuses()));
        }
    }

}
