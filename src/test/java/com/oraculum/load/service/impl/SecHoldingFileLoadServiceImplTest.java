package com.oraculum.load.service.impl;

import com.oraculum.load.dto.DataBatchCompleteEvent;
import com.oraculum.load.dto.DataFileReadyEvent;
import com.oraculum.load.dto.LoadParquetDto;
import com.oraculum.load.service.SecHoldingDeltaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecHoldingFileLoadServiceImplTest {

    @Mock
    private PostgresParquetFileLoader postgresParquetFileLoader;

    @Mock
    private SecHoldingDeltaService secHoldingDeltaService;

    @Captor
    private ArgumentCaptor<LoadParquetDto> dtoCaptor;

    private SecHoldingFileLoadServiceImpl loadService;

    @BeforeEach
    void setUp() {
        loadService = new SecHoldingFileLoadServiceImpl(postgresParquetFileLoader, secHoldingDeltaService);
    }

    @Test
    void merge_callsPostgresLoader_withCorrectDto() {
        DataFileReadyEvent event = new DataFileReadyEvent(
                "oraculum.data_file_ready",
                "sec_13f_holding",
                "holdings.parquet",
                null,
                null,
                1,
                "corr-1",
                "chk123",
                50,
                null,
                ZonedDateTime.now()
        );
        when(postgresParquetFileLoader.resolveAndValidatePath(any())).thenReturn("holdings-path.parquet");

        loadService.merge(event);

        verify(postgresParquetFileLoader).loadParquetIntoTargetTable(dtoCaptor.capture());
        LoadParquetDto capturedDto = dtoCaptor.getValue();

        assertThat(capturedDto.targetTableName()).isEqualTo("t_sec_holding");
        assertThat(capturedDto.stagingTableName()).startsWith("staging_t_sec_holding_");
        assertThat(capturedDto.parquetFilePath()).isEqualTo("holdings-path.parquet");
        assertThat(capturedDto.hasStatementData()).isFalse();
        assertThat(capturedDto.loadSql()).contains("INSERT INTO t_sec_holding");
        assertThat(capturedDto.loadSql()).contains("ON CONFLICT (accession_number, cusip, COALESCE(option_type, ''), report_period)");
    }

    @Test
    void postBatchComplete_delegatesToDeltaService() {
        LocalDate reportPeriod = LocalDate.of(2026, 3, 31);
        DataBatchCompleteEvent event = new DataBatchCompleteEvent(
                "sec_13f_holding",
                "corr-1",
                5,
                reportPeriod,
                ZonedDateTime.now()
        );

        loadService.postBatchComplete(event);

        verify(secHoldingDeltaService).recalculateDelta(reportPeriod);
    }
}
