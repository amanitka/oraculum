package com.oraculum.load.service.impl;

import com.oraculum.load.dto.DataFileReadyEvent;
import com.oraculum.load.dto.LoadParquetDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InsiderTransactionTickerFileLoadServiceImplTest {

    @Mock
    private PostgresParquetFileLoader postgresParquetFileLoader;

    @Captor
    private ArgumentCaptor<LoadParquetDto> dtoCaptor;

    private InsiderTransactionTickerFileLoadServiceImpl loadService;

    @BeforeEach
    void setUp() {
        loadService = new InsiderTransactionTickerFileLoadServiceImpl(postgresParquetFileLoader);
    }

    @Test
    void merge_callsPostgresLoader_withCorrectDto() {
        DataFileReadyEvent event = new DataFileReadyEvent("event", "test_dataset", "file.parquet", "template", "variant", 1, "run1", "checksum", 100, null, java.time.ZonedDateTime.now());
        when(postgresParquetFileLoader.resolveAndValidatePath(any())).thenReturn("test-path.parquet");

        loadService.merge(event);

        verify(postgresParquetFileLoader).loadParquetIntoTargetTable(dtoCaptor.capture());
        LoadParquetDto capturedDto = dtoCaptor.getValue();

        assertThat(capturedDto.targetTableName()).isEqualTo("t_insider_transaction_ticker");
        assertThat(capturedDto.stagingTableName()).startsWith("staging_t_insider_transaction_ticker_");
        assertThat(capturedDto.parquetFilePath()).isEqualTo("test-path.parquet");
        assertThat(capturedDto.hasStatementData()).isFalse();
        assertThat(capturedDto.loadSql()).contains("INSERT INTO t_insider_transaction_ticker AS dest");
        assertThat(capturedDto.loadSql()).contains("ON CONFLICT (id, filing_date)");
    }
}
