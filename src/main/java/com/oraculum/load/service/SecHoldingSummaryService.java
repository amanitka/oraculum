package com.oraculum.load.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.LocalDate;

/**
 * Service responsible for orchestrating incremental SEC 13F holding summaries
 * by calling the {@code recalculate_sec_holding_summary} stored procedure.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecHoldingSummaryService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void recalculateSummary(LocalDate currentPeriod) {
        Assert.notNull(currentPeriod, "currentPeriod must not be null");
        log.info("Starting SEC holding summary recalculation for period: {}", currentPeriod);

        jdbcTemplate.update("CALL recalculate_sec_holding_summary(?::DATE)", currentPeriod);
        log.info("SEC holding summary recalculation completed for period: {}", currentPeriod);

        refreshStatistics(currentPeriod);
    }

    private void refreshStatistics(LocalDate period) {
        String partitionName = "t_sec_holding_summary_" + period.getYear();
        if (partitionExists(partitionName)) {
            log.info("Analyzing partition '{}' to refresh query planner statistics...", partitionName);
            jdbcTemplate.execute("ANALYZE " + partitionName);
            log.info("Analyzed partition '{}' is finished", partitionName);
        }
    }

    private boolean partitionExists(String partitionName) {
        Boolean exists = jdbcTemplate.queryForObject("SELECT to_regclass(?) IS NOT NULL", Boolean.class, partitionName);
        return Boolean.TRUE.equals(exists);
    }
}
