package com.oraculum.database.maintenance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "oraculum.database.maintenance.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseMaintenanceScheduler {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(cron = "${oraculum.database.maintenance.vacuum-cron}")
    public void runVacuum() {
        log.info("Starting scheduled database VACUUM ANALYZE...");
        try {
            jdbcTemplate.execute("VACUUM ANALYZE");
            log.info("Database VACUUM ANALYZE completed successfully.");
        } catch (Exception e) {
            log.error("Scheduled database VACUUM ANALYZE failed", e);
        }
    }

    @Scheduled(cron = "${oraculum.database.maintenance.partition-cron}")
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
}
