package com.oraculum.database.service;

import com.oraculum.database.domain.PartitionConfig;
import com.oraculum.database.domain.PartitionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseMaintenanceService {

    private final JdbcTemplate jdbcTemplate;

    public void runVacuum() {
        log.info("Starting database VACUUM ANALYZE...");
        try {
            jdbcTemplate.execute("VACUUM ANALYZE");
            log.info("Database VACUUM ANALYZE completed successfully.");
        } catch (Exception e) {
            log.error("Database VACUUM ANALYZE failed", e);
        }
    }

    public void runPartitionManagement() {
        log.info("Starting scheduled database partition management (creation & purging)...");
        try {
            for (PartitionConfig config : PartitionConfig.values()) {
                String createFunction = config.getType() == PartitionType.MONTHLY ? "create_monthly_partitions" : "create_yearly_partitions";
                
                String createSql = String.format(
                    "SELECT %s('%s', (NOW() - INTERVAL '1 month')::DATE, (NOW() + INTERVAL '%d months')::DATE);",
                    createFunction, config.getTableName(), config.getMonthsAhead()
                );
                jdbcTemplate.execute(createSql);
                log.info("Pre-created partitions for {} ({} months ahead)", config.getTableName(), config.getMonthsAhead());

                String purgeSql = String.format(
                    "SELECT purge_old_partitions('%s', (NOW() - INTERVAL '%d months')::DATE);",
                    config.getTableName(), config.getMonthsToKeep()
                );
                jdbcTemplate.execute(purgeSql);
                log.info("Purged old partitions for {} (older than {} months)", config.getTableName(), config.getMonthsToKeep());
            }
            log.info("Database partition management completed successfully.");
        } catch (Exception e) {
            log.error("Error during database partition management", e);
        }
    }

    public void refreshMaterializedViews() {
        log.info("Starting refresh of materialized views from event...");
        try {
            refreshAndAnalyze("mv_company_financial_ratios");
            refreshAndAnalyze("mv_share_price_signals_recent");
            refreshAndAnalyze("mv_company_overview");
            log.info("Materialized views refresh & statistics update completed successfully.");
        } catch (Exception e) {
            log.error("Materialized views refresh failed", e);
        }
    }

    private void refreshAndAnalyze(String viewName) {
        log.info("Refreshing materialized view {}", viewName);
        jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY " + viewName);
        log.info("Updating statistics for materialized view {}", viewName);
        jdbcTemplate.execute("ANALYZE " + viewName);
    }
}
