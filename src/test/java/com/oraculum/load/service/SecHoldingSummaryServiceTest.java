package com.oraculum.load.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecHoldingSummaryServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private SecHoldingSummaryService summaryService;

    @BeforeEach
    void setUp() {
        summaryService = new SecHoldingSummaryService(jdbcTemplate);
    }

    @Test
    void recalculateSummary_whenPeriodNull_throwsException() {
        assertThatThrownBy(() -> summaryService.recalculateSummary(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currentPeriod must not be null");
    }

    @Test
    void recalculateSummary_withSpecificPeriod_callsProcedureAndAnalyzesPartition() {
        LocalDate current = LocalDate.of(2026, 3, 31);
        String expectedPartition = "t_sec_holding_summary_2026";

        when(jdbcTemplate.queryForObject(
                eq("SELECT to_regclass(?) IS NOT NULL"),
                eq(Boolean.class),
                eq(expectedPartition)
        )).thenReturn(true);

        summaryService.recalculateSummary(current);

        verify(jdbcTemplate).update("CALL recalculate_sec_holding_summary(?::DATE)", current);
        verify(jdbcTemplate).execute("ANALYZE " + expectedPartition);
    }

    @Test
    void recalculateSummary_whenPartitionDoesNotExist_skipsAnalyze() {
        LocalDate current = LocalDate.of(2026, 3, 31);
        String expectedPartition = "t_sec_holding_summary_2026";

        when(jdbcTemplate.queryForObject(
                eq("SELECT to_regclass(?) IS NOT NULL"),
                eq(Boolean.class),
                eq(expectedPartition)
        )).thenReturn(false);

        summaryService.recalculateSummary(current);

        verify(jdbcTemplate).update("CALL recalculate_sec_holding_summary(?::DATE)", current);
        verify(jdbcTemplate, never()).execute(startsWith("ANALYZE"));
    }
}
