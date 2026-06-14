package com.oraculum.database.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseMaintenanceServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DatabaseMaintenanceService databaseMaintenanceService;

    @Test
    void runVacuum_executesVacuumAnalyze() {
        databaseMaintenanceService.runVacuum();

        verify(jdbcTemplate).execute("VACUUM ANALYZE");
    }

    @Test
    void runVacuum_handlesExceptionGracefully() {
        doThrow(new RuntimeException("DB Error")).when(jdbcTemplate).execute(anyString());

        // Should not throw exception, just log error
        databaseMaintenanceService.runVacuum();

        verify(jdbcTemplate).execute("VACUUM ANALYZE");
    }

    @Test
    void runPartitionManagement_executesManagementScript() {
        databaseMaintenanceService.runPartitionManagement();

        verify(jdbcTemplate).execute(contains("create_monthly_partitions('t_share_price'"));
        verify(jdbcTemplate).execute(contains("purge_old_partitions('t_news'"));
    }

    @Test
    void refreshMaterializedViews_executesRefreshAndAnalyze() {
        databaseMaintenanceService.refreshMaterializedViews();

        verify(jdbcTemplate).execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_company_financial_ratios");
        verify(jdbcTemplate).execute("ANALYZE mv_company_financial_ratios");
        verify(jdbcTemplate).execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_share_price_signals_recent");
        verify(jdbcTemplate).execute("ANALYZE mv_share_price_signals_recent");
    }
}
