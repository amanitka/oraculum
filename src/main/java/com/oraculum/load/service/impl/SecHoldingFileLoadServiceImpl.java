package com.oraculum.load.service.impl;

import com.oraculum.load.domain.Dataset;
import com.oraculum.load.dto.DataBatchCompleteEvent;
import com.oraculum.load.dto.DataFileReadyEvent;
import com.oraculum.load.dto.LoadParquetDto;
import com.oraculum.load.service.ParquetFileLoadService;
import com.oraculum.load.service.SecHoldingDeltaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Loads SEC 13F institutional holdings (INFOTABLE) into {@code t_sec_holding}.
 *
 * <p>Called N times per quarter — once per Parquet chunk. The
 * {@link #postBatchComplete} hook fires exactly once (triggered by
 * {@code DataBatchCompleteEvent}) and delegates to {@link SecHoldingDeltaService}
 * to refresh quarter-over-quarter position changes.
 */
@Slf4j
@Component(Dataset.SEC_13F_HOLDING)
@RequiredArgsConstructor
public class SecHoldingFileLoadServiceImpl implements ParquetFileLoadService {

    private static final String TARGET_TABLE = "t_sec_holding";

    private static final String INSERT_SQL = """
            INSERT INTO t_sec_holding
              (cik,
               accession_number,
               report_period,
               filing_date,
               manager_name,
               issuer_name,
               class_title,
               cusip,
               value_usd,
               shares_or_prn_amount,
               shares_or_prn_type,
               option_type,
               investment_discretion,
               voting_auth_sole,
               voting_auth_shared,
               voting_auth_none,
               created_at)
            SELECT
               src.cik,
               src.accession_number,
               CAST(src.report_period AS DATE),
               CAST(src.filing_date   AS DATE),
               src.manager_name,
               src.issuer_name,
               src.class_title,
               src.cusip,
               COALESCE(CAST(src.value_usd            AS BIGINT), 0),
               COALESCE(CAST(src.shares_or_prn_amount AS BIGINT), 0),
               src.shares_or_prn_type,
               NULLIF(src.option_type, ''),
               src.investment_discretion,
               COALESCE(CAST(src.voting_auth_sole   AS BIGINT), 0),
               COALESCE(CAST(src.voting_auth_shared AS BIGINT), 0),
               COALESCE(CAST(src.voting_auth_none   AS BIGINT), 0),
               CURRENT_TIMESTAMP
            FROM %s AS src;
            """;

    private final PostgresParquetFileLoader postgresParquetFileLoader;
    private final SecHoldingDeltaService secHoldingDeltaService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void merge(DataFileReadyEvent event) {
        boolean isFirstPart = isFirstChunk(event);
        var staging = PostgresParquetFileLoader.getStagingTableName(TARGET_TABLE);
        var dto = LoadParquetDto.builder()
                .targetTableName(TARGET_TABLE)
                .stagingTableName(staging)
                .parquetFilePath(postgresParquetFileLoader.resolveAndValidatePath(event))
                .loadSql(INSERT_SQL.formatted(staging))
                .hasStatementData(false)
                .preLoadAction(stagingTable -> truncatePartitionIfFirstChunk(stagingTable, isFirstPart))
                .build();
        postgresParquetFileLoader.loadParquetIntoTargetTable(dto);
    }

    private boolean isFirstChunk(DataFileReadyEvent event) {
        return Boolean.TRUE.equals(event.isFirstPart());
    }

    private void truncatePartitionIfFirstChunk(String stagingTable, boolean isFirstPart) {
        if (!isFirstPart) {
            return;
        }
        String sql = "SELECT quarterly_partition_name('t_sec_holding', CAST(report_period AS DATE)) FROM "
                + stagingTable + " WHERE report_period IS NOT NULL LIMIT 1";
        String partitionName = jdbcTemplate.queryForObject(sql, String.class);
        if (partitionName != null) {
            log.info("First chunk detected: truncating existing partition '{}'", partitionName);
            jdbcTemplate.execute("TRUNCATE TABLE " + partitionName);
        }
    }

    @Override
    public void postBatchComplete(DataBatchCompleteEvent event) {
        LocalDate current = event.reportPeriod();
        secHoldingDeltaService.recalculateDelta(current);
    }
}
