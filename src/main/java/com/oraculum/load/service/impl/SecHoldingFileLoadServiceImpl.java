package com.oraculum.load.service.impl;

import com.oraculum.load.domain.Dataset;
import com.oraculum.load.dto.DataBatchCompleteEvent;
import com.oraculum.load.dto.DataFileReadyEvent;
import com.oraculum.load.dto.LoadParquetDto;
import com.oraculum.load.service.ParquetFileLoadService;
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
 * {@code DataBatchCompleteEvent}) and runs {@code sp_compute_sec_holding_delta}
 * to refresh quarter-over-quarter position changes.
 */
@Slf4j
@Component(Dataset.SEC_13F_HOLDING)
@RequiredArgsConstructor
public class SecHoldingFileLoadServiceImpl implements ParquetFileLoadService {

    private static final String TARGET_TABLE = "t_sec_holding";

    private static final String UPSERT_SQL = """
            INSERT INTO t_sec_holding (
                cik, accession_number, period_of_report, filing_date,
                manager_name, issuer_name, class_title, cusip,
                value_usd, shares_or_prn_amount, shares_or_prn_type, option_type,
                investment_discretion,
                voting_auth_sole, voting_auth_shared, voting_auth_none
            )
            SELECT
                src.cik,
                src.accession_number,
                CAST(src.period_of_report AS DATE),
                CAST(src.filing_date     AS DATE),
                src.manager_name,
                src.issuer_name,
                src.class_title,
                src.cusip,
                CAST(src.value_usd            AS BIGINT),
                CAST(src.shares_or_prn_amount AS BIGINT),
                src.shares_or_prn_type,
                NULLIF(src.option_type, ''),
                src.investment_discretion,
                CAST(src.voting_auth_sole   AS BIGINT),
                CAST(src.voting_auth_shared AS BIGINT),
                CAST(src.voting_auth_none   AS BIGINT)
            FROM %s AS src
            ON CONFLICT (accession_number, cusip, COALESCE(option_type, ''), period_of_report)
            DO NOTHING;
            """;

    private final PostgresParquetFileLoader postgresParquetFileLoader;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void merge(DataFileReadyEvent event) {
        var staging = PostgresParquetFileLoader.getStagingTableName(TARGET_TABLE);
        var dto = LoadParquetDto.builder()
                .targetTableName(TARGET_TABLE)
                .stagingTableName(staging)
                .parquetFilePath(postgresParquetFileLoader.resolveAndValidatePath(event))
                .loadSql(UPSERT_SQL.formatted(staging))
                .hasStatementData(false)
                .build();
        postgresParquetFileLoader.loadParquetIntoTargetTable(dto);
    }

    /**
     * Runs the delta stored procedure once after all chunks have landed.
     * Previous period = current minus one quarter.
     */
    @Override
    public void postBatchComplete(DataBatchCompleteEvent event) {
        LocalDate current  = event.periodOfReport();
        LocalDate previous = current.minusMonths(3);
        log.info("Running sp_compute_sec_holding_delta: current={}, previous={}", current, previous);
        jdbcTemplate.update("CALL sp_compute_sec_holding_delta(?, ?)", current, previous);
        log.info("sp_compute_sec_holding_delta completed for period {}", current);
    }
}
