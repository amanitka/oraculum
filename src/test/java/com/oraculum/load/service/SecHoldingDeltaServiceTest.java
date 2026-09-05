package com.oraculum.load.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecHoldingDeltaServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    private SecHoldingDeltaService deltaService;

    @BeforeEach
    void setUp() {
        deltaService = new SecHoldingDeltaService(jdbcTemplate, namedJdbcTemplate);
    }

    @Test
    void recalculateDelta_truncatesExistingPartition_andInsertsDeltaRows() {
        LocalDate current = LocalDate.of(2026, 3, 31);
        LocalDate previous = LocalDate.of(2025, 12, 31);
        String deltaPartitionName = "t_sec_holding_delta_2026_q1";
        String holdingPartitionCur = "t_sec_holding_2026_q1";
        String holdingPartitionPrv = "t_sec_holding_2025_q4";

        when(jdbcTemplate.queryForObject(
                eq("SELECT quarterly_partition_name(?, ?)"),
                eq(String.class),
                eq("t_sec_holding_delta"),
                eq(current)
        )).thenReturn(deltaPartitionName);

        when(jdbcTemplate.queryForObject(
                eq("SELECT quarterly_partition_name(?, ?)"),
                eq(String.class),
                eq("t_sec_holding"),
                eq(current)
        )).thenReturn(holdingPartitionCur);

        when(jdbcTemplate.queryForObject(
                eq("SELECT quarterly_partition_name(?, ?)"),
                eq(String.class),
                eq("t_sec_holding"),
                eq(previous)
        )).thenReturn(holdingPartitionPrv);

        when(jdbcTemplate.queryForObject(
                eq("SELECT to_regclass(?) IS NOT NULL"),
                eq(Boolean.class),
                anyString()
        )).thenReturn(true);

        when(namedJdbcTemplate.update(
                anyString(),
                eq(Map.of("currentPeriod", current, "previousPeriod", previous))
        )).thenReturn(150);

        int rows = deltaService.recalculateDelta(current, previous);

        assertThat(rows).isEqualTo(150);
        verify(jdbcTemplate).execute("TRUNCATE TABLE " + deltaPartitionName);
        verify(jdbcTemplate).execute("ANALYZE " + holdingPartitionCur);
        verify(jdbcTemplate).execute("ANALYZE " + holdingPartitionPrv);
        verify(jdbcTemplate).execute("ANALYZE " + deltaPartitionName);
        verify(jdbcTemplate).execute("SET LOCAL synchronous_commit = off");
        verify(jdbcTemplate).execute("SET LOCAL work_mem = '128MB'");
        verify(jdbcTemplate).execute("SET LOCAL max_parallel_workers_per_gather = 4");
    }

    @Test
    void recalculateDelta_whenPartitionDoesNotExist_skipsTruncate() {
        LocalDate current = LocalDate.of(2026, 3, 31);
        LocalDate previous = LocalDate.of(2025, 12, 31);
        String partitionName = "t_sec_holding_delta_2026_q1";

        when(jdbcTemplate.queryForObject(
                eq("SELECT quarterly_partition_name(?, ?)"),
                eq(String.class),
                anyString(),
                any(LocalDate.class)
        )).thenReturn(partitionName);

        when(jdbcTemplate.queryForObject(
                eq("SELECT to_regclass(?) IS NOT NULL"),
                eq(Boolean.class),
                eq(partitionName)
        )).thenReturn(false);

        when(namedJdbcTemplate.update(anyString(), anyMap())).thenReturn(42);

        int rows = deltaService.recalculateDelta(current, previous);

        assertThat(rows).isEqualTo(42);
        verify(jdbcTemplate).query(contains("create_quarterly_partitions"), any(ResultSetExtractor.class), eq("t_sec_holding_delta"), eq(current), eq(current));
        verify(jdbcTemplate, never()).execute(startsWith("TRUNCATE TABLE"));
        verify(jdbcTemplate, never()).execute(startsWith("ANALYZE"));
    }

    @Test
    void recalculateDelta_singleParamOverload_derivesPreviousQuarter() {
        LocalDate current = LocalDate.of(2026, 6, 30);
        LocalDate expectedPrevious = LocalDate.of(2026, 3, 30);

        when(jdbcTemplate.queryForObject(
                eq("SELECT quarterly_partition_name(?, ?)"),
                eq(String.class),
                anyString(),
                any(LocalDate.class)
        )).thenReturn(null);

        when(namedJdbcTemplate.update(
                anyString(),
                eq(Map.of("currentPeriod", current, "previousPeriod", expectedPrevious))
        )).thenReturn(80);

        int rows = deltaService.recalculateDelta(current);

        assertThat(rows).isEqualTo(80);
    }
}
