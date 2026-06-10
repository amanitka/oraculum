package com.oraculum.database.scheduler;

import com.oraculum.database.service.DatabaseMaintenanceService;
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

    private final DatabaseMaintenanceService databaseMaintenanceService;

    @Scheduled(cron = "${oraculum.database.maintenance.vacuum-cron}")
    public void runVacuum() {
        databaseMaintenanceService.runVacuum();
    }

    @Scheduled(cron = "${oraculum.database.maintenance.partition-cron}")
    public void runPartitionManagement() {
        databaseMaintenanceService.runPartitionManagement();
    }

    @Scheduled(cron = "${oraculum.database.maintenance.mv-refresh-cron:-}")
    public void refreshMaterializedViews() {
        databaseMaintenanceService.refreshMaterializedViews();
    }
}
