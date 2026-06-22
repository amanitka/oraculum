package com.oraculum.load.service.impl;

import com.oraculum.load.dto.LoadParquetDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

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
        String testPath = "test-path.parquet";

        loadService.merge(testPath);

        verify(postgresParquetFileLoader).loadParquetIntoTargetTable(dtoCaptor.capture());
        LoadParquetDto capturedDto = dtoCaptor.getValue();

        assertThat(capturedDto.targetTableName()).isEqualTo("t_insider_transaction_ticker");
        assertThat(capturedDto.stagingTableName()).startsWith("staging_t_insider_transaction_ticker_");
        assertThat(capturedDto.parquetFilePath()).isEqualTo("test-path.parquet"); // PostgresParquetFileLoader.normalizeAndValidate does nothing on a simple string but passes it
        assertThat(capturedDto.hasStatementData()).isFalse();
        assertThat(capturedDto.loadSql()).contains("INSERT INTO t_insider_transaction_ticker AS dest");
        assertThat(capturedDto.loadSql()).contains("ON CONFLICT (id, filing_date)");
    }
}
