package com.oraculum.database.service;

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
            jdbcTemplate.execute("""
                    DO $$
                    BEGIN
                        -- Pre-create partitions for the next 3 months to avoid insertion errors
                        PERFORM create_monthly_partitions('t_share_price', (NOW() - INTERVAL '1 month')::DATE, (NOW() + INTERVAL '3 months')::DATE);
                        PERFORM create_yearly_partitions('t_news', (NOW() - INTERVAL '1 month')::DATE, (NOW() + INTERVAL '2 years')::DATE);
                        PERFORM create_yearly_partitions('t_news_ticker', (NOW() - INTERVAL '1 month')::DATE, (NOW() + INTERVAL '2 years')::DATE);
                    
                        -- Purge old partition data (older than 3 years)
                        PERFORM purge_old_partitions('t_news_ticker', '3 years', 'yearly');
                        PERFORM purge_old_partitions('t_news', '3 years', 'yearly');
                    END;
                    $$;
                    """);
            log.info("Database partition management completed successfully.");
        } catch (Exception e) {
            log.error("Scheduled database partition management failed", e);
        }
    }

    public void refreshMaterializedViews() {
        log.info("Starting refresh of materialized views from event...");
        try {
            log.info("Refreshing materialized view mv_company_financial_ratios");
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_company_financial_ratios");
            log.info("Updating statistics for materialized view mv_company_financial_ratios");
            jdbcTemplate.execute("ANALYZE mv_company_financial_ratios");
            log.info("Refreshing materialized view mv_share_price_signals_recent");
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_share_price_signals_recent");
            log.info("Updating statistics for materialized view mv_share_price_signals_recent");
            jdbcTemplate.execute("ANALYZE mv_share_price_signals_recent");
            log.info("Materialized views refresh & statistics update completed successfully.");
        } catch (Exception e) {
            log.error("Materialized views refresh failed", e);
        }
    }
}
