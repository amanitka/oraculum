package com.oraculum.load.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
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
        String partitionName = "t_sec_holding_delta_2026_q1";

        when(jdbcTemplate.queryForObject(
                eq("SELECT quarterly_partition_name('t_sec_holding_delta', ?)"),
                eq(String.class),
                eq(current)
        )).thenReturn(partitionName);

        when(jdbcTemplate.queryForObject(
                eq("SELECT to_regclass(?) IS NOT NULL"),
                eq(Boolean.class),
                eq(partitionName)
        )).thenReturn(true);

        when(namedJdbcTemplate.update(
                anyString(),
                eq(Map.of("currentPeriod", current, "previousPeriod", previous))
        )).thenReturn(150);

        int rows = deltaService.recalculateDelta(current, previous);

        assertThat(rows).isEqualTo(150);
        verify(jdbcTemplate).execute("TRUNCATE TABLE " + partitionName);
    }

    @Test
    void recalculateDelta_whenPartitionDoesNotExist_skipsTruncate() {
        LocalDate current = LocalDate.of(2026, 3, 31);
        LocalDate previous = LocalDate.of(2025, 12, 31);
        String partitionName = "t_sec_holding_delta_2026_q1";

        when(jdbcTemplate.queryForObject(
                eq("SELECT quarterly_partition_name('t_sec_holding_delta', ?)"),
                eq(String.class),
                eq(current)
        )).thenReturn(partitionName);

        when(jdbcTemplate.queryForObject(
                eq("SELECT to_regclass(?) IS NOT NULL"),
                eq(Boolean.class),
                eq(partitionName)
        )).thenReturn(false);

        when(namedJdbcTemplate.update(anyString(), anyMap())).thenReturn(42);

        int rows = deltaService.recalculateDelta(current, previous);

        assertThat(rows).isEqualTo(42);
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void recalculateDelta_singleParamOverload_derivesPreviousQuarter() {
        LocalDate current = LocalDate.of(2026, 6, 30);
        LocalDate expectedPrevious = LocalDate.of(2026, 3, 30);

        when(jdbcTemplate.queryForObject(
                eq("SELECT quarterly_partition_name('t_sec_holding_delta', ?)"),
                eq(String.class),
                eq(current)
        )).thenReturn(null);

        when(namedJdbcTemplate.update(
                anyString(),
                eq(Map.of("currentPeriod", current, "previousPeriod", expectedPrevious))
        )).thenReturn(80);

        int rows = deltaService.recalculateDelta(current);

        assertThat(rows).isEqualTo(80);
    }
}
