package com.oraculum.load.service.impl;

import com.oraculum.common.config.OraculumProperties;
import com.oraculum.load.dto.DataFileReadyEvent;
import com.oraculum.load.dto.LoadParquetDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;

import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class SecFilerFileLoadServiceImplTest {

    @Mock
    private PostgresParquetFileLoader postgresParquetFileLoader;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private OraculumProperties properties;

    @Captor
    private ArgumentCaptor<LoadParquetDto> dtoCaptor;

    private SecFilerFileLoadServiceImpl loadService;

    @BeforeEach
    void setUp() {
        loadService = new SecFilerFileLoadServiceImpl(postgresParquetFileLoader, jdbcTemplate, properties);
    }

    @Test
    void merge_callsPostgresLoader_withCorrectDto() {
        DataFileReadyEvent event = new DataFileReadyEvent(
                "oraculum.data_file_ready",
                "sec_13f_filer",
                "filers.parquet",
                null,
                null,
                1,
                "corr-1",
                "chk123",
                100,
                null,
                ZonedDateTime.now()
        );
        when(postgresParquetFileLoader.resolveAndValidatePath(any())).thenReturn("filers-path.parquet");

        loadService.merge(event);

        verify(postgresParquetFileLoader).loadParquetIntoTargetTable(dtoCaptor.capture());
        LoadParquetDto capturedDto = dtoCaptor.getValue();

        assertThat(capturedDto.targetTableName()).isEqualTo("t_sec_filer");
        assertThat(capturedDto.stagingTableName()).startsWith("staging_t_sec_filer_");
        assertThat(capturedDto.parquetFilePath()).isEqualTo("filers-path.parquet");
        assertThat(capturedDto.hasStatementData()).isFalse();
        assertThat(capturedDto.loadSql()).contains("INSERT INTO t_sec_filer");
        assertThat(capturedDto.loadSql()).contains("ON CONFLICT (cik)\n" + "DO UPDATE SET");
    }

    @Test
    void postProcess_promotesTier1Ciks() {
        when(properties.data().sec13f().tier1Ciks()).thenReturn(List.of("0001067983", "0001649339"));
        when(jdbcTemplate.update(eq("""
                UPDATE t_sec_filer
                   SET tier = 1,
                       updated_at = NOW()
                 WHERE cik = ANY(?)
                   AND tier != 1;
                """), any(PreparedStatementSetter.class))).thenReturn(2);

        DataFileReadyEvent event = new DataFileReadyEvent(
                "oraculum.data_file_ready",
                "sec_13f_filer",
                "filers.parquet",
                null,
                null,
                1,
                "corr-1",
                "chk123",
                100,
                null,
                ZonedDateTime.now()
        );

        loadService.postProcess(event);

        verify(jdbcTemplate).update(contains("UPDATE t_sec_filer"), any(PreparedStatementSetter.class));

    }

    @Test
    void postProcess_whenNoTier1Ciks_doesNothing() {
        when(properties.data().sec13f().tier1Ciks()).thenReturn(List.of());

        DataFileReadyEvent event = new DataFileReadyEvent(
                "oraculum.data_file_ready",
                "sec_13f_filer",
                "filers.parquet",
                null,
                null,
                1,
                "corr-1",
                "chk123",
                100,
                null,
                ZonedDateTime.now()
        );

        loadService.postProcess(event);

        verify(jdbcTemplate, never()).update(any(String.class), any(PreparedStatementSetter.class));
    }
}
