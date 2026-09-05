package com.oraculum.load.service.impl;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.load.domain.Dataset;
import com.oraculum.load.dto.DataFileReadyEvent;
import com.oraculum.load.dto.LoadParquetDto;
import com.oraculum.load.service.ParquetFileLoadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Loads SEC 13F filer metadata (SUBMISSION + COVERPAGE) into {@code t_sec_filer}.
 *
 * <p>Filers are always published as a single Parquet file per quarter, so
 * {@link #postProcess} (called after every merge) is used to promote known
 * institutional managers to {@code TIER_1} using the CIK list from config.
 */
@Slf4j
@Component(Dataset.SEC_13F_FILER)
@RequiredArgsConstructor
public class SecFilerFileLoadServiceImpl implements ParquetFileLoadService {

    private static final String TARGET_TABLE = "t_sec_filer";

    private static final String UPSERT_SQL = """
            INSERT INTO t_sec_filer
              (cik,
               manager_name,
               is_active,
               tier,
               last_filing_date,
               last_processed_accession,
               created_at,
               updated_at)
            SELECT
               src.cik,
               src.manager_name,
               TRUE,
               2,
               CAST(src.filing_date AS DATE),
               src.accession_number,
               CURRENT_TIMESTAMP,
               CURRENT_TIMESTAMP
            FROM %s AS src
            ON CONFLICT (cik)
            DO UPDATE SET
               manager_name               = EXCLUDED.manager_name,
               last_filing_date           = EXCLUDED.last_filing_date,
               last_processed_accession   = EXCLUDED.last_processed_accession,
               is_active                  = TRUE,
               updated_at                 = CURRENT_TIMESTAMP;
            """;

    private static final String PROMOTE_TIER1_SQL = """
            UPDATE t_sec_filer
               SET tier = 1,
                   updated_at = NOW()
             WHERE cik = ANY(?)
               AND tier != 1;
            """;

    private final PostgresParquetFileLoader postgresParquetFileLoader;
    private final JdbcTemplate jdbcTemplate;
    private final OraculumProperties properties;

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
     * Promotes known institutional managers to TIER_1 after the filer data lands.
     * Safe to call repeatedly — the UPDATE only touches rows where tier differs.
     */
    @Override
    public void postProcess(DataFileReadyEvent event) {
        List<String> tier1Ciks = properties.data().sec13f().tier1Ciks();
        if (tier1Ciks.isEmpty()) {
            return;
        }
        String[] cikArray = tier1Ciks.toArray(String[]::new);
        int promoted = jdbcTemplate.update(PROMOTE_TIER1_SQL,
                (ps) -> ps.setArray(1, ps.getConnection().createArrayOf("text", cikArray)));
        log.info("Promoted {} filer(s) to Tier 1", promoted);
    }
}
