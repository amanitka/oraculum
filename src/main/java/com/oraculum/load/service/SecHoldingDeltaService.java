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
            WITH ranked_holdings AS (
                SELECT
                    cik,
                    report_period,
                    cusip,
                    issuer_name,
                    COALESCE(option_type, '') AS option_type,
                    value_usd,
                    shares_or_prn_amount,
                    DENSE_RANK() OVER (
                        PARTITION BY cik, report_period
                        ORDER BY filing_date DESC, accession_number DESC
                    ) AS rn
                FROM t_sec_holding
                WHERE report_period IN (:previousPeriod, :currentPeriod)
            ),
            holdings_summary AS (
                SELECT
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
            )
            INSERT INTO t_sec_holding_delta
              (cik,
               report_period,
               cusip,
               issuer_name,
               value_usd_current,
               value_usd_previous,
               value_usd_change,
               shares_current,
               shares_previous,
               shares_change,
               is_new_position,
               is_closed_position)
            SELECT
               cur.cik,
               cur.report_period,
               cur.cusip,
               cur.issuer_name,
               cur.value_usd                                                    AS value_usd_current,
               prv.value_usd                                                    AS value_usd_previous,
               cur.value_usd            - COALESCE(prv.value_usd, 0)            AS value_usd_change,
               cur.shares_or_prn_amount                                         AS shares_current,
               prv.shares_or_prn_amount                                         AS shares_previous,
               cur.shares_or_prn_amount - COALESCE(prv.shares_or_prn_amount, 0) AS shares_change,
               prv.cik IS NULL                                                  AS is_new_position,
               FALSE                                                            AS is_closed_position
            FROM holdings_summary cur
            LEFT JOIN holdings_summary prv ON prv.cik         = cur.cik
                                       AND prv.cusip       = cur.cusip
                                       AND prv.option_type = cur.option_type
                                       AND prv.report_period = :previousPeriod
            WHERE cur.report_period = :currentPeriod
              UNION ALL
            SELECT
                prv.cik,
                :currentPeriod,
                prv.cusip,
                prv.issuer_name,
                0                         AS value_usd_current,
                prv.value_usd             AS value_usd_previous,
                -prv.value_usd            AS value_usd_change,
                0                         AS shares_current,
                prv.shares_or_prn_amount  AS shares_previous,
                -prv.shares_or_prn_amount AS shares_change,
                FALSE                     AS is_new_position,
                TRUE                      AS is_closed_position
            FROM holdings_summary prv
            WHERE prv.report_period = :previousPeriod
              AND NOT EXISTS (
                  SELECT 1
                  FROM holdings_summary cur
                  WHERE cur.cik         = prv.cik
                    AND cur.cusip       = prv.cusip
                    AND cur.option_type = prv.option_type
                    AND cur.report_period = :currentPeriod
              )
            ON CONFLICT (cik, cusip, report_period) DO NOTHING;
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
        truncateDeltaPartition(currentPeriod);
        Map<String, Object> params = Map.of(
                "currentPeriod", currentPeriod,
                "previousPeriod", previousPeriod
        );
        int rows = namedJdbcTemplate.update(COMPUTE_DELTA_SQL, params);
        log.info("SEC holding delta recalculation completed: {} rows inserted for period {}", rows, currentPeriod);
        return rows;
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
        String partitionName = jdbcTemplate.queryForObject(
                "SELECT quarterly_partition_name('t_sec_holding_delta', ?)",
                String.class,
                currentPeriod
        );
        if (partitionName != null) {
            Boolean exists = jdbcTemplate.queryForObject(
                    "SELECT to_regclass(?) IS NOT NULL",
                    Boolean.class,
                    partitionName
            );
            if (Boolean.TRUE.equals(exists)) {
                log.info("Truncating partition '{}'", partitionName);
                jdbcTemplate.execute("TRUNCATE TABLE " + partitionName);
            }
        }
    }
}
