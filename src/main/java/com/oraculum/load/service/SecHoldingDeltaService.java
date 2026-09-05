package com.oraculum.load.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

/**
 * Service responsible for calculating quarter-over-quarter SEC 13F holding position deltas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecHoldingDeltaService {

    private static final String COMPUTE_DELTA_SQL = """
            WITH ranked_holdings AS (SELECT
                                        cik,
                                        report_period,
                                        cusip,
                                        issuer_name,
                                        COALESCE(option_type, '') AS option_type,
                                        value_usd,
                                        shares_or_prn_amount,
                                        DENSE_RANK() OVER (
                                            PARTITION BY cik, report_period
                                            ORDER BY filing_date DESC, accession_number DESC) AS rn
                                     FROM t_sec_holding
                                     WHERE report_period IN (:previousPeriod, :currentPeriod)
                                     ),
                 holdings_summary AS (SELECT
                                         cik,
                                         report_period,
                                         cusip,
                                         MAX(issuer_name)          AS issuer_name,
                                         option_type,
                                         SUM(value_usd)            AS value_usd,
                                         SUM(shares_or_prn_amount) AS shares_or_prn_amount
                                      FROM ranked_holdings
                                      WHERE rn = 1
                                      GROUP BY
                                          cik,
                                          report_period,
                                          cusip,
                                          option_type
                                      ),
                 current_holdings AS (SELECT
                                         cik,
                                         cusip,
                                         option_type,
                                         issuer_name,
                                         value_usd,
                                         shares_or_prn_amount
                                      FROM holdings_summary
                                      WHERE report_period = :currentPeriod
                                      ),
                 previous_holdings AS (SELECT
                                          cik,
                                          cusip,
                                          option_type,
                                          issuer_name,
                                          value_usd,
                                          shares_or_prn_amount
                                       FROM holdings_summary
                                       WHERE report_period = :previousPeriod
                                       )
            INSERT INTO t_sec_holding_delta
              (cik,
               report_period,
               cusip,
               issuer_name,
               option_type,
               value_usd_current,
               value_usd_previous,
               value_usd_change,
               shares_current,
               shares_previous,
               shares_change,
               is_new_position,
               is_closed_position)
            SELECT
               COALESCE(cur.cik, prv.cik),
               :currentPeriod,
               COALESCE(cur.cusip, prv.cusip),
               COALESCE(cur.issuer_name, prv.issuer_name),
               NULLIF(COALESCE(cur.option_type, prv.option_type), ''),
               COALESCE(cur.value_usd, 0)                                                       AS value_usd_current,
               prv.value_usd                                                                    AS value_usd_previous,
               COALESCE(cur.value_usd, 0) - COALESCE(prv.value_usd, 0)                         AS value_usd_change,
               COALESCE(cur.shares_or_prn_amount, 0)                                            AS shares_current,
               prv.shares_or_prn_amount                                                         AS shares_previous,
               COALESCE(cur.shares_or_prn_amount, 0) - COALESCE(prv.shares_or_prn_amount, 0) AS shares_change,
               (prv.cik IS NULL)                                                                AS is_new_position,
               (cur.cik IS NULL)                                                                AS is_closed_position
            FROM current_holdings cur
            FULL OUTER JOIN previous_holdings prv ON prv.cik = cur.cik
                                                 AND prv.cusip = cur.cusip
                                                 AND prv.option_type = cur.option_type;
            """;

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    /**
     * Recalculates holding deltas for the given quarter against the specified previous quarter.
     *
     * @param currentPeriod  the report period for the current quarter
     * @param previousPeriod the report period for the previous quarter
     * @return the number of delta records computed and stored
     */
    @Transactional
    public int recalculateDelta(LocalDate currentPeriod, LocalDate previousPeriod) {
        log.info("Starting SEC holding delta recalculation: current={}, previous={}", currentPeriod, previousPeriod);
        ensureDeltaPartition(currentPeriod);
        truncateDeltaPartition(currentPeriod);
        refreshStatistics(currentPeriod, previousPeriod);
        tuneSessionForBatch();

        Map<String, Object> params = Map.of("currentPeriod", currentPeriod, "previousPeriod", previousPeriod);
        log.info("Executing SEC holding delta query (COMPUTE_DELTA_SQL) for current={}, previous={}...", currentPeriod, previousPeriod);
        int rows = namedJdbcTemplate.update(COMPUTE_DELTA_SQL, params);
        log.info("SEC holding delta recalculation completed: {} rows inserted for period {}", rows, currentPeriod);
        return rows;
    }

    private void tuneSessionForBatch() {
        jdbcTemplate.execute("SET LOCAL synchronous_commit = off");
        jdbcTemplate.execute("SET LOCAL work_mem = '128MB'");
        jdbcTemplate.execute("SET LOCAL max_parallel_workers_per_gather = 4");
    }

    private void ensureDeltaPartition(LocalDate currentPeriod) {
        log.info("Ensuring delta partition exists for period {}", currentPeriod);
        jdbcTemplate.query(
                "SELECT create_quarterly_partitions(?, ?::DATE, ?::DATE)",
                _ -> null,
                "t_sec_holding_delta",
                currentPeriod,
                currentPeriod
        );
    }

    private void refreshStatistics(LocalDate currentPeriod, LocalDate previousPeriod) {
        analyzeQuarterlyPartition("t_sec_holding", currentPeriod);
        if (previousPeriod != null && !previousPeriod.equals(currentPeriod)) {
            analyzeQuarterlyPartition("t_sec_holding", previousPeriod);
        }
        analyzeQuarterlyPartition("t_sec_holding_delta", currentPeriod);
    }

    private void analyzeQuarterlyPartition(String tableName, LocalDate period) {
        String partitionName = getPartitionName(tableName, period);
        if (partitionName != null && partitionExists(partitionName)) {
            log.info("Analyzing partition '{}' to refresh query planner statistics...", partitionName);
            jdbcTemplate.execute("ANALYZE " + partitionName);
            log.info("Analyzed partition '{}' is finished", partitionName);
        }
    }

    /**
     * Recalculates holding deltas for the given quarter, deriving the previous quarter
     * as 3 months prior.
     *
     * @param currentPeriod the report period for the current quarter
     * @return the number of delta records computed and stored
     */
    @Transactional
    public int recalculateDelta(LocalDate currentPeriod) {
        return recalculateDelta(currentPeriod, currentPeriod.minusMonths(3));
    }

    private void truncateDeltaPartition(LocalDate currentPeriod) {
        String partitionName = getPartitionName("t_sec_holding_delta", currentPeriod);
        if (partitionName != null && partitionExists(partitionName)) {
            log.info("Truncating partition '{}'...", partitionName);
            jdbcTemplate.execute("TRUNCATE TABLE " + partitionName);
            log.info("Successfully truncated partition '{}'", partitionName);
        }
    }

    private String getPartitionName(String tableName, LocalDate period) {
        return jdbcTemplate.queryForObject("SELECT quarterly_partition_name(?, ?)", String.class, tableName, period);
    }

    private boolean partitionExists(String partitionName) {
        Boolean exists = jdbcTemplate.queryForObject("SELECT to_regclass(?) IS NOT NULL", Boolean.class, partitionName);
        return Boolean.TRUE.equals(exists);
    }
}
